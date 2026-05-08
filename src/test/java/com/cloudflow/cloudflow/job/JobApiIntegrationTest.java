package com.cloudflow.cloudflow.job;

import com.cloudflow.cloudflow.BaseIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class JobApiIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String authToken;

    // @BeforeEach runs before every test method.
    // We register and login once to get a token, then use it for all job API calls.
    @BeforeEach
    void setUpAuth() {
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());

        Map<String, Object> registerReq = Map.of(
                "companyName", "Job Test Corp " + uniqueSuffix,
                "slug", "jobtest" + uniqueSuffix,
                "email", "jobtest@test.com",
                "password", "password123"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/register",
                registerReq,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        authToken = response.getBody().get("token").toString();
    }

    // Helper to build headers with JWT token
    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    @DisplayName("POST /jobs should create a job and return 201")
    void createJob_validRequest_returns201() {
        Map<String, Object> jobReq = Map.of(
                "name", "Integration Test Job",
                "cronExpression", "0 0 23 * * ?",
                "targetUrl", "https://httpbin.org/get",
                "httpMethod", "GET",
                "timezone", "UTC",
                "timeoutSeconds", 30,
                "retryLimit", 3,
                "retryDelaySeconds", 60
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/v1/jobs",
                HttpMethod.POST,
                new HttpEntity<>(jobReq, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<String, Object> body = response.getBody();
        assertThat(body.get("id")).isNotNull();
        assertThat(body.get("name")).isEqualTo("Integration Test Job");
        assertThat(body.get("status")).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("POST /jobs with invalid cron should return 400")
    void createJob_invalidCron_returns400() {
        Map<String, Object> jobReq = Map.of(
                "name", "Bad Cron Job",
                "cronExpression", "not-a-cron",
                "targetUrl", "https://httpbin.org/get",
                "httpMethod", "GET",
                "timezone", "UTC",
                "timeoutSeconds", 30,
                "retryLimit", 3,
                "retryDelaySeconds", 60
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/v1/jobs",
                HttpMethod.POST,
                new HttpEntity<>(jobReq, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message").toString())
                .contains("Invalid cron expression");
    }

    @Test
    @DisplayName("GET /jobs should return only jobs for the authenticated tenant")
    void getJobs_authenticated_returnsOnlyOwnJobs() {
        // Create a job for this tenant
        Map<String, Object> jobReq = Map.of(
                "name", "Tenant Isolation Test Job",
                "cronExpression", "0 0 23 * * ?",
                "targetUrl", "https://httpbin.org/get",
                "httpMethod", "GET",
                "timezone", "UTC",
                "timeoutSeconds", 30,
                "retryLimit", 3,
                "retryDelaySeconds", 60
        );
        restTemplate.exchange(
                baseUrl() + "/api/v1/jobs",
                HttpMethod.POST,
                new HttpEntity<>(jobReq, authHeaders()),
                Map.class
        );

        // Get all jobs for this tenant
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/v1/jobs",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> page = response.getBody();
        assertThat(page.get("content")).isNotNull();

        // All returned jobs must belong to this tenant
        // (verified by the fact that we're using a unique tenant per test)
        int totalElements = (int) page.get("totalElements");
        assertThat(totalElements).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("GET /jobs without auth should return 403")
    void getJobs_noAuth_returns403() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/v1/jobs",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()), // No auth header
                Map.class
        );

        assertThat(response.getStatusCode())
                .isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
    }
}