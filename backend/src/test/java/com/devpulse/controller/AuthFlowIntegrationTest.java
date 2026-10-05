package com.devpulse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
                .andExpect(status().isBadRequest());
    }
}
