package com.devpulse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.TestPropertySource;
import jakarta.servlet.http.Cookie;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end check of the real Phase 3 flow: register -> login -> use the
 * issued JWT to create and list a project. Runs against the in-memory H2
 * database with the RoleSeeder populating ADMIN/DEVELOPER/VIEWER at startup,
 * exactly like a local `mvn spring-boot:run` would.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "devpulse.auth-rate-limit.enabled=false")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldKeepApiDocsPrivateByDefault() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRegisterLoginAndCreateProjectWithIssuedToken() throws Exception {
        String email = "integration-" + UUID.randomUUID() + "@example.com";

        Map<String, String> registerBody = Map.of(
                "firstName", "Integration",
                "lastName", "Tester",
                "email", email,
                "password", "supersecret123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated());

        Map<String, String> loginBody = Map.of("email", email, "password", "supersecret123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), Map.class);
        String accessToken = (String) loginResponse.get("token");
        assertThat(accessToken).isNotBlank();

        Map<String, String> projectBody = Map.of(
                "name", "billing-service",
                "description", "Payments API",
                "environment", "development"
        );

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectBody)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectProjectCreationWithoutToken() throws Exception {
        Map<String, String> projectBody = Map.of(
                "name", "unauthorized-service",
                "environment", "development"
        );

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectBody)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectLoginWithWrongPassword() throws Exception {
        String email = "wrongpass-" + UUID.randomUUID() + "@example.com";

        Map<String, String> registerBody = Map.of(
                "firstName", "Wrong",
                "lastName", "Pass",
                "email", email,
                "password", "correct-password"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated());

        Map<String, String> loginBody = Map.of("email", email, "password", "wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRotateRefreshTokenAndRevokeItsFamilyWhenAnOldTokenIsReused() throws Exception {
        String email = "rotation-" + UUID.randomUUID() + "@example.com";
        registerAndLogin(email);
        String initialRefreshToken = login(email);

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("devpulse_refresh", initialRefreshToken)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(refreshResult.getResponse().getContentAsString()).doesNotContain("refreshToken");
        assertThat(refreshResult.getResponse().getHeader("Set-Cookie"))
                .contains("HttpOnly", "Path=/api/auth", "SameSite=Lax");
        String rotatedRefreshToken = refreshResult.getResponse().getCookie("devpulse_refresh").getValue();
        assertThat(rotatedRefreshToken).isNotBlank().isNotEqualTo(initialRefreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("devpulse_refresh", initialRefreshToken)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("devpulse_refresh", rotatedRefreshToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNormalizeEmailAndRejectCaseInsensitiveDuplicate() throws Exception {
        String email = "MixedCase-" + UUID.randomUUID() + "@example.com";
        Map<String, String> body = Map.of(
                "firstName", "Mixed",
                "lastName", "Case",
                "email", email,
                "password", "correct-password"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains(email.toLowerCase(java.util.Locale.ROOT)));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email.toLowerCase(java.util.Locale.ROOT),
                                "password", "correct-password"))))
                .andExpect(status().isOk());

        Map<String, String> duplicate = Map.of(
                "firstName", "Duplicate",
                "lastName", "User",
                "email", email.toUpperCase(java.util.Locale.ROOT),
                "password", "another-password"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRevokeRefreshTokenFamilyOnLogout() throws Exception {
        String email = "logout-" + UUID.randomUUID() + "@example.com";
        registerAndLogin(email);
        String refreshToken = login(email);

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("devpulse_refresh", refreshToken)))
                .andExpect(status().isNoContent())
                .andExpect(result -> assertThat(result.getResponse().getHeader("Set-Cookie"))
                        .contains("Max-Age=0", "HttpOnly", "Path=/api/auth"));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("devpulse_refresh", refreshToken)))
                .andExpect(status().isBadRequest());
    }

    private void registerAndLogin(String email) throws Exception {
        Map<String, String> registerBody = Map.of(
                "firstName", "Integration",
                "lastName", "Tester",
                "email", email,
                "password", "supersecret123"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", "supersecret123"))))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        assertThat(response.containsKey("refreshToken")).isFalse();
        assertThat(result.getResponse().getHeader("Set-Cookie"))
                .contains("HttpOnly", "Path=/api/auth", "SameSite=Lax");
        return result.getResponse().getCookie("devpulse_refresh").getValue();
    }

}
