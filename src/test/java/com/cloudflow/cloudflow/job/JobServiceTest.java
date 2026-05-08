package com.cloudflow.cloudflow.job;

import com.cloudflow.cloudflow.job.dto.CreateJobRequest;
import com.cloudflow.cloudflow.job.dto.JobResponse;
import com.cloudflow.cloudflow.kafka.KafkaProducerService;
import com.cloudflow.cloudflow.multitenancy.TenantContext;
import com.cloudflow.cloudflow.scheduler.JobSchedulerService;
import com.cloudflow.cloudflow.tenant.Tenant;
import com.cloudflow.cloudflow.tenant.TenantRepository;
import com.cloudflow.cloudflow.user.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// LENIENT strictness allows stubs in @BeforeEach that not every test uses
// Without this, Mockito throws UnnecessaryStubbingException for shared setUp stubs
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobServiceTest {

    @Mock private JobRepository jobRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private CronValidator cronValidator;
    @Mock private JobSchedulerService jobSchedulerService; // ← needed for createJob→scheduleJob
    @Mock private KafkaProducerService kafkaProducerService; // ← needed for triggerJobNow

    @InjectMocks
    private JobService jobService;

    private UUID tenantId;
    private UUID userId;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        TenantContext.setTenantId(tenantId);

        // LENIENT allows these stubs to exist even if a specific test doesn't call them
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(userId.toString());
        SecurityContext secCtx = mock(SecurityContext.class);
        lenient().when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Test Company");
        tenant.setSlug("testco");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("createJob should save job and return JobResponse")
    void createJob_validRequest_returnsJobResponse() {
        CreateJobRequest request = new CreateJobRequest();
        request.setName("Test Job");
        request.setCronExpression("0 * * * * ?");
        request.setTargetUrl("https://httpbin.org/get");
        request.setHttpMethod("GET");
        request.setTimezone("UTC");
        request.setTimeoutSeconds(30);
        request.setRetryLimit(3);
        request.setRetryDelaySeconds(60);

        doNothing().when(cronValidator).validateOrThrow(anyString());
        when(jobRepository.existsByNameAndTenantId(anyString(), any(UUID.class))).thenReturn(false);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        doNothing().when(jobSchedulerService).scheduleJob(any(Job.class));

        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            job.setId(UUID.randomUUID());
            return job;
        });

        JobResponse response = jobService.createJob(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Job");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");

        verify(jobRepository).save(any(Job.class));
        verify(jobSchedulerService).scheduleJob(any(Job.class));
    }

    @Test
    @DisplayName("createJob should throw when cron expression is invalid")
    void createJob_invalidCron_throwsException() {
        CreateJobRequest request = new CreateJobRequest();
        request.setName("Bad Job");
        request.setCronExpression("not-a-cron");
        request.setTargetUrl("https://example.com");
        request.setHttpMethod("GET");
        request.setTimezone("UTC");
        request.setTimeoutSeconds(30);
        request.setRetryLimit(3);
        request.setRetryDelaySeconds(60);

        doThrow(new IllegalArgumentException("Invalid cron expression: 'not-a-cron'"))
                .when(cronValidator).validateOrThrow("not-a-cron");

        assertThatThrownBy(() -> jobService.createJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid cron expression");

        verify(jobRepository, never()).save(any());
    }

    @Test
    @DisplayName("createJob should throw when job name already exists for tenant")
    void createJob_duplicateName_throwsException() {
        CreateJobRequest request = new CreateJobRequest();
        request.setName("Duplicate Job");
        request.setCronExpression("0 * * * * ?");
        request.setTargetUrl("https://example.com");
        request.setHttpMethod("GET");
        request.setTimezone("UTC");
        request.setTimeoutSeconds(30);
        request.setRetryLimit(3);
        request.setRetryDelaySeconds(60);

        doNothing().when(cronValidator).validateOrThrow(anyString());
        when(jobRepository.existsByNameAndTenantId("Duplicate Job", tenantId)).thenReturn(true);

        assertThatThrownBy(() -> jobService.createJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(jobRepository, never()).save(any());
    }

    @Test
    @DisplayName("getJobById should throw when job belongs to different tenant")
    void getJobById_wrongTenant_throwsException() {
        UUID jobId = UUID.randomUUID();

        when(jobRepository.findByIdAndTenantId(jobId, tenantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJobById(jobId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Job not found");
    }
}
//$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
//$env:Path="$env:JAVA_HOME\bin;$env:Path"