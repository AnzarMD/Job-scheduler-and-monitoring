package com.cloudflow.cloudflow.auth;

import com.cloudflow.cloudflow.BaseIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

// Extends BaseIntegrationTest — gets the shared PostgreSQL container
// and full Spring context automatically
class AuthIntegrationTest extends BaseIntegrationTest {

    // TestRestTemplate is a test-friendly HTTP client provided by Spring Boot.
    // Unlike RestTemplate, it doesn't throw on 4xx/5xx — it returns the response.
    // This lets us assert on error responses.
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("POST /auth/register should create tenant and return JWT token")
    void register_validRequest_returns201WithToken() {
        Map<String, Object> request = Map.of(
                "companyName", "Integration Test Corp " + System.currentTimeMillis(),
                "slug", "inttest" + System.currentTimeMillis(),
                "email", "admin@inttest.com",
                "password", "password123",
                "firstName", "Test",
                "lastName", "User"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/register",
                request,
                Map.class
        );

        // Assert HTTP status
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Assert response body
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("token")).isNotNull();
        assertThat(body.get("token").toString()).isNotEmpty();
        assertThat(body.get("email")).isEqualTo("admin@inttest.com");
        assertThat(body.get("role")).isEqualTo("OWNER");
    }

    @Test
    @DisplayName("POST /auth/login should return JWT token for valid credentials")
    void login_validCredentials_returns200WithToken() {
        // First register a user
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        Map<String, Object> registerReq = Map.of(
                "companyName", "Login Test Corp " + uniqueSuffix,
                "slug", "logintest" + uniqueSuffix,
                "email", "login@test.com",
                "password", "password123"
        );
        restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/register", registerReq, Map.class
        );

        // Then log in
        Map<String, Object> loginReq = Map.of(
                "slug", "logintest" + uniqueSuffix,
                "email", "login@test.com",
                "password", "password123"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/login",
                loginReq,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("token")).isNotNull();
    }

    @Test
    @DisplayName("POST /auth/login with wrong password should return 400")
    void login_wrongPassword_returns400() {
        // Register first
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        Map<String, Object> registerReq = Map.of(
                "companyName", "Wrong Pass Corp " + uniqueSuffix,
                "slug", "wrongpass" + uniqueSuffix,
                "email", "wrong@test.com",
                "password", "correctpassword"
        );
        restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/register", registerReq, Map.class
        );

        // Try to login with wrong password
        Map<String, Object> loginReq = Map.of(
                "slug", "wrongpass" + uniqueSuffix,
                "email", "wrong@test.com",
                "password", "WRONG_PASSWORD"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/login",
                loginReq,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message").toString())
                .contains("Invalid credentials");
    }
}