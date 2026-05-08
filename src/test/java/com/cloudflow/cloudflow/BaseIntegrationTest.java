package com.cloudflow.cloudflow;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

// @SpringBootTest loads the full Spring application context — same as running the app.
// webEnvironment = RANDOM_PORT starts the embedded server on a random available port.
// This avoids port conflicts when running tests on CI servers.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
public abstract class BaseIntegrationTest {

    // @Container marks this as a Testcontainers-managed container.
    // static = one container shared across ALL test methods in ALL subclasses.
    // Without static, a new container starts for every test class — very slow.
    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                    .withDatabaseName("cloudflow_test")
                    .withUsername("test_user")
                    .withPassword("test_pass");

    @Container
    static final KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    // @DynamicPropertySource overrides Spring properties at test time.
    // This is how we point Spring Boot at the Testcontainers PostgreSQL
    // instead of the real database configured in application.yml.
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Override datasource to point to the test container
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Override Kafka bootstrap servers
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // Use in-memory data store for Quartz in tests (simpler)
        registry.add("spring.quartz.job-store-type", () -> "memory");

        // Disable Redis for integration tests (use mock)
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }

    // The random port that the embedded server starts on
    // Inject this into test classes to build URLs
    @LocalServerPort
    protected int port;

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}