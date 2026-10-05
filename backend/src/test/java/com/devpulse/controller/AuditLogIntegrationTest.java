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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditLogIntegrationTest {

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

    @Test
    void shouldRecordSensitiveActionsAndRestrictPagedAuditReads() throws Exception {
        String ownerEmail = "audit-owner-" + UUID.randomUUID() + "@example.com";
        String ownerToken = registerAndLogin(ownerEmail);
        Long projectId = createProject(ownerToken);

        String memberEmail = "audit-member-" + UUID.randomUUID() + "@example.com";
        String memberToken = registerAndLogin(memberEmail);

        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", memberEmail,
                                "role", "VIEWER"))))
                .andExpect(status().isCreated());

        MvcResult keyResult = mockMvc.perform(post("/api/projects/" + projectId + "/ingestion-keys")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "audit-test-key"))))
                .andExpect(status().isCreated())
                .andReturn();
        Map<?, ?> createdKey = objectMapper.readValue(keyResult.getResponse().getContentAsString(), Map.class);
        String secretKey = (String) createdKey.get("key");
        Long keyId = Long.valueOf(createdKey.get("id").toString());

        mockMvc.perform(put("/api/projects/" + projectId + "/members/"
                                + userRepository.findByEmail(memberEmail).orElseThrow().getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "DEVELOPER"))))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/projects/" + projectId + "/members/"
                                + userRepository.findByEmail(memberEmail).orElseThrow().getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/projects/" + projectId + "/ingestion-keys/" + keyId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/" + projectId + "/audit-logs")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(5));

        MvcResult pageResult = mockMvc.perform(get("/api/projects/" + projectId + "/audit-logs")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(pageResult.getResponse().getContentAsString())
                .doesNotContain(secretKey)
                .contains("INGESTION_KEY_CREATED", "INGESTION_KEY_REVOKED", "PROJECT_MEMBER_ADDED",
                        "PROJECT_MEMBER_ROLE_CHANGED", "PROJECT_MEMBER_REMOVED");

        mockMvc.perform(get("/api/projects/" + projectId + "/audit-logs")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());

        String intruderToken = registerAndLogin("audit-intruder-" + UUID.randomUUID() + "@example.com");
        mockMvc.perform(get("/api/projects/" + projectId + "/audit-logs")
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());

        String adminEmail = "audit-admin-" + UUID.randomUUID() + "@example.com";
        String adminToken = registerAndLogin(adminEmail);
        User admin = userRepository.findByEmail(adminEmail).orElseThrow();
        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
        admin.setRole(adminRole);
        userRepository.save(admin);
        adminToken = login(adminEmail);

        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("size", "1000"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.action == 'AUTH_LOGIN')]").exists())
                .andExpect(jsonPath("$.content[?(@.action == 'INGESTION_KEY_CREATED')]").exists())
                .andExpect(jsonPath("$.content[?(@.action == 'PROJECT_MEMBER_ADDED')]").exists());

        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Audit",
                                "lastName", "Tester",
                                "email", email,
                                "password", "supersecret123"))))
                .andExpect(status().isCreated());
        return login(email);
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "supersecret123"))))
                .andExpect(status().isOk())
                .andReturn();
        return (String) objectMapper.readValue(result.getResponse().getContentAsString(), Map.class)
                .get("token");
    }

    private Long createProject(String ownerToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "audit-project-" + UUID.randomUUID(),
                                "environment", "development"))))
                .andExpect(status().isCreated())
                .andReturn();
        return Long.valueOf(objectMapper.readValue(result.getResponse().getContentAsString(), Map.class)
                .get("id").toString());
    }
}
