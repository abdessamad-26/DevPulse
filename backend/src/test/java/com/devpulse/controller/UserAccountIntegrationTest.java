package com.devpulse.controller;

import com.devpulse.entity.Role;
import com.devpulse.entity.User;
import com.devpulse.repository.RoleRepository;
import com.devpulse.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "devpulse.auth-rate-limit.enabled=false")
class UserAccountIntegrationTest {

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
    void shouldUpdateOwnProfileAndRevokeSessionsAfterPasswordChange() throws Exception {
        String email = "account-" + UUID.randomUUID() + "@example.com";
        register(email);
        Session session = login(email, "supersecret123");

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + session.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Updated",
                                "lastName", "Account"))))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("Updated", "Account", email));

        mockMvc.perform(post("/api/users/me/password")
                        .header("Authorization", "Bearer " + session.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "supersecret123",
                                "newPassword", "a-new-long-password"))))
                .andExpect(status().isNoContent())
                .andExpect(result -> assertThat(result.getResponse().getHeader("Set-Cookie"))
                        .contains("Max-Age=0", "HttpOnly", "Path=/api/auth"));

        if (session.cookieBased()) {
            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("devpulse_refresh", session.refreshToken())))
                    .andExpect(status().isBadRequest());
        } else {
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("refreshToken", session.refreshToken()))))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "supersecret123"))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "a-new-long-password"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRestrictAdministrationAndApplyRoleChangesImmediately() throws Exception {
        User admin = createAdmin("admin-" + UUID.randomUUID() + "@example.com");
        String targetEmail = "managed-" + UUID.randomUUID() + "@example.com";
        register(targetEmail);
        Session adminSession = login(admin.getEmail(), "supersecret123");
        Session targetSession = login(targetEmail, "supersecret123");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + targetSession.accessToken()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminSession.accessToken()))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).contains(targetEmail));

        Long targetId = userRepository.findByEmail(targetEmail).orElseThrow().getId();
        mockMvc.perform(patch("/api/admin/users/{userId}", targetId)
                        .header("Authorization", "Bearer " + adminSession.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "VIEWER", "enabled", true))))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).contains("VIEWER"));

        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + adminSession.accessToken()))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("ADMIN_USER_UPDATED", "\"actorUserId\":" + admin.getId()));

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + targetSession.accessToken()))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/users/{userId}", admin.getId())
                        .header("Authorization", "Bearer " + adminSession.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "DEVELOPER", "enabled", true))))
                .andExpect(status().isConflict());
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Integration",
                                "lastName", "Tester",
                                "email", email,
                                "password", "supersecret123"))))
                .andExpect(status().isCreated());
    }

    private Session login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Cookie refreshCookie = result.getResponse().getCookie("devpulse_refresh");
        String refreshToken = refreshCookie == null
                ? (String) response.get("refreshToken")
                : refreshCookie.getValue();
        return new Session((String) response.get("token"), refreshToken, refreshCookie != null);
    }

    private User createAdmin(String email) {
        Role role = roleRepository.findByName("ADMIN").orElseThrow();
        User admin = new User();
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode("supersecret123"));
        admin.setFirstName("Admin");
        admin.setLastName("Tester");
        admin.setRole(role);
        return userRepository.save(admin);
    }

    private record Session(String accessToken, String refreshToken, boolean cookieBased) {}
}
