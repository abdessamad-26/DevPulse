package com.devpulse.controller;

import com.devpulse.entity.Role;
import com.devpulse.entity.User;
import com.devpulse.repository.RoleRepository;
import com.devpulse.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of the Service / Incident / Deployment slice added on
 * top of the Phase 3 auth flow: register two separate users, let one of them
 * build out a project with a service, an incident and a deployment, and
 * confirm the other user is rejected (403) when trying to touch the same
 * project's resources.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProjectResourcesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String registerAndLogin(String emailPrefix) throws Exception {
        String email = emailPrefix + "-" + UUID.randomUUID() + "@example.com";

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
        return (String) loginResponse.get("token");
    }

    private Long createProject(String token, String name) throws Exception {
        Map<String, String> body = Map.of(
                "name", name,
                "description", "Created by integration test",
                "environment", "development"
        );
        MvcResult result = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> project = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return Long.valueOf(project.get("id").toString());
    }

    @Test
    void shouldManageServiceIncidentAndDeploymentForOwnedProject() throws Exception {
        String token = registerAndLogin("owner");
        Long projectId = createProject(token, "billing-platform");

        // --- Service ---
        Map<String, String> serviceBody = Map.of("name", "billing-api", "type", "http");
        MvcResult serviceResult = mockMvc.perform(post("/api/projects/" + projectId + "/services")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(serviceBody)))
                .andExpect(status().isCreated())
                .andReturn();
        Map<?, ?> service = objectMapper.readValue(serviceResult.getResponse().getContentAsString(), Map.class);
        Long serviceId = Long.valueOf(service.get("id").toString());

        mockMvc.perform(get("/api/projects/" + projectId + "/services")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // --- Incident ---
        Map<String, Object> incidentBody = new HashMap<>();
        incidentBody.put("projectId", projectId);
        incidentBody.put("serviceId", serviceId);
        incidentBody.put("title", "Latency spike on billing-api");
        incidentBody.put("severity", "high");

        MvcResult incidentResult = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incidentBody)))
                .andExpect(status().isCreated())
                .andReturn();
        Map<?, ?> incident = objectMapper.readValue(incidentResult.getResponse().getContentAsString(), Map.class);
        assertThat(incident.get("status")).isEqualTo("OPEN");
        Long incidentId = Long.valueOf(incident.get("id").toString());

        mockMvc.perform(get("/api/incidents").param("projectId", projectId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/incidents/" + incidentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        Map<String, String> resolveBody = Map.of("status", "resolved", "rootCause", "Missing DB index");
        mockMvc.perform(patch("/api/incidents/" + incidentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveBody)))
                .andExpect(status().isOk());

        // --- Deployment ---
        Map<String, String> deploymentBody = Map.of(
                "projectId", projectId.toString(),
                "version", "v1.0.0",
                "environment", "production",
                "status", "success"
        );
        mockMvc.perform(post("/api/deployments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deploymentBody)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/deployments").param("projectId", projectId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectAccessToAnotherUsersProjectResources() throws Exception {
        String ownerToken = registerAndLogin("owner2");
        Long projectId = createProject(ownerToken, "someone-elses-project");

        String otherToken = registerAndLogin("intruder");

        Map<String, String> serviceBody = Map.of("name", "billing-api");
        mockMvc.perform(post("/api/projects/" + projectId + "/services")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(serviceBody)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/incidents").param("projectId", projectId.toString())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectServiceAndProjectCreationByViewerRole() throws Exception {
        String email = "viewer-" + UUID.randomUUID() + "@example.com";
        Role viewerRole = roleRepository.findByName("VIEWER")
                .orElseThrow(() -> new IllegalStateException("VIEWER role not seeded - check RoleSeeder"));

        User viewer = new User();
        viewer.setEmail(email);
        viewer.setPassword(passwordEncoder.encode("supersecret123"));
        viewer.setFirstName("View");
        viewer.setLastName("Only");
        viewer.setRole(viewerRole);
        userRepository.save(viewer);

        Map<String, String> loginBody = Map.of("email", email, "password", "supersecret123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), Map.class);
        String viewerToken = (String) loginResponse.get("token");

        Map<String, String> projectBody = Map.of(
                "name", "viewer-should-not-create-this",
                "environment", "development"
        );
        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectBody)))
                .andExpect(status().isForbidden());

        // A VIEWER can still list (read) their own - empty - project list.
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());
    }
}
