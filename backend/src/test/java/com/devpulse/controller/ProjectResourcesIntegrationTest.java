package com.devpulse.controller;

import com.devpulse.entity.Role;
import com.devpulse.entity.User;
import com.devpulse.repository.RoleRepository;
import com.devpulse.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
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
        return registerAndLoginEmail(email);
    }

    private String registerAndLoginEmail(String email) throws Exception {
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
    void shouldEnforceRolesIndependentlyForEachProjectMember() throws Exception {
        String ownerToken = registerAndLogin("membership-owner");
        Long viewerProjectId = createProject(ownerToken, "viewer-project");
        Long developerProjectId = createProject(ownerToken, "developer-project");
        String memberEmail = "project-member-" + UUID.randomUUID() + "@example.com";
        String memberToken = registerAndLoginEmail(memberEmail);
        Long memberId = userRepository.findByEmail(memberEmail).orElseThrow().getId();

        mockMvc.perform(post("/api/projects/" + viewerProjectId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", memberEmail, "role", "VIEWER"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/projects/" + developerProjectId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", memberEmail, "role", "DEVELOPER"))))
                .andExpect(status().isCreated());

        MvcResult projectsResult = mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> projectsPage = objectMapper.readValue(
                projectsResult.getResponse().getContentAsString(), Map.class);
        List<?> projects = (List<?>) projectsPage.get("content");
        assertThat(projectsPage.get("totalElements").toString()).isEqualTo("2");
        assertThat(projects).extracting(project -> ((Map<?, ?>) project).get("id").toString())
                .containsExactlyInAnyOrder(viewerProjectId.toString(), developerProjectId.toString());

        MvcResult membersResult = mockMvc.perform(get("/api/projects/" + viewerProjectId + "/members")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> members = objectMapper.readValue(
                membersResult.getResponse().getContentAsString(), new TypeReference<>() {});
        assertThat(members).anySatisfy(member -> {
            assertThat(member.get("userId").toString()).isEqualTo(memberId.toString());
            assertThat(member.get("role")).isEqualTo("VIEWER");
        });

        mockMvc.perform(get("/api/projects/" + viewerProjectId + "/services")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/projects/" + viewerProjectId + "/services")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "read-only-service"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/projects/" + developerProjectId + "/services")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "developer-service"))))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/projects/" + developerProjectId + "/members/" + memberId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "VIEWER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/projects/" + developerProjectId + "/members/" + memberId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "VIEWER"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/projects/" + developerProjectId + "/services")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "no-longer-writable"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/projects/" + developerProjectId + "/members/" + memberId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/projects/" + developerProjectId + "/services")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRestrictIngestionApiKeysToTheirProjectAndIngestionEndpoints() throws Exception {
        String ownerToken = registerAndLogin("ingestion-key-owner");
        Long projectId = createProject(ownerToken, "key-protected-project");
        Long otherProjectId = createProject(ownerToken, "other-key-protected-project");

        MvcResult createdResult = mockMvc.perform(post("/api/projects/" + projectId + "/ingestion-keys")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "production collector"))))
                .andExpect(status().isCreated())
                .andReturn();
        Map<?, ?> createdKey = objectMapper.readValue(createdResult.getResponse().getContentAsString(), Map.class);
        String apiKey = (String) createdKey.get("key");
        Long keyId = Long.valueOf(createdKey.get("id").toString());
        assertThat(apiKey).startsWith("dp_ing_");

        MvcResult listedResult = mockMvc.perform(get("/api/projects/" + projectId + "/ingestion-keys")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(listedResult.getResponse().getContentAsString())
                .contains("production collector", createdKey.get("prefix").toString())
                .doesNotContain(apiKey, "keyHash");

        mockMvc.perform(post("/api/projects/" + projectId + "/metrics")
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"points":[{"serviceName":"checkout","metricName":"cpu","value":72.5}]}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/projects/" + projectId + "/logs")
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entries":[{"serviceName":"checkout","level":"INFO","message":"ingestion ok"}]}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/projects/" + otherProjectId + "/metrics")
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"points":[{"metricName":"cpu","value":72.5}]}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/projects/" + projectId + "/metrics")
                        .header("X-API-Key", apiKey))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/projects/" + projectId + "/services")
                        .header("X-API-Key", apiKey))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/projects/" + projectId + "/metrics")
                        .header("X-API-Key", "dp_ing_invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"points":[{"metricName":"cpu","value":72.5}]}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/projects/" + projectId + "/ingestion-keys/" + keyId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/projects/" + projectId + "/logs")
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entries":[{"message":"revoked key"}]}
                                """))
                .andExpect(status().isUnauthorized());
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
