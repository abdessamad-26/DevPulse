package com.devpulse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the observability slice: alert rule -> metric ingestion -> automatic
 * alert triggering (AlertEvaluationService), plus log ingestion and filtered
 * search. This is the part most likely to have subtle wiring bugs (JPA
 * Specification, nested project_id filtering), so it's worth its own
 * end-to-end test rather than relying on unit tests alone.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ObservabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin(String emailPrefix) throws Exception {
        String email = emailPrefix + "-" + UUID.randomUUID() + "@example.com";
        Map<String, String> registerBody = Map.of(
                "firstName", "Integration", "lastName", "Tester", "email", email, "password", "supersecret123");
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

    private Long createProject(String token) throws Exception {
        Map<String, String> body = Map.of("name", "observability-demo", "environment", "production");
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
    void shouldTriggerAlertWhenIngestedMetricBreachesRule() throws Exception {
        String token = registerAndLogin("obs-owner");
        Long projectId = createProject(token);

        Map<String, Object> ruleBody = Map.of(
                "name", "High CPU",
                "metric", "cpu_usage_percent",
                "operator", ">",
                "threshold", 80.0,
                "severity", "high"
        );
        mockMvc.perform(post("/api/projects/" + projectId + "/alert-rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ruleBody)))
                .andExpect(status().isCreated());

        Map<String, Object> point = new HashMap<>();
        point.put("metricName", "cpu_usage_percent");
        point.put("value", 93.4);
        Map<String, Object> ingestBody = Map.of("points", List.of(point));

        mockMvc.perform(post("/api/projects/" + projectId + "/metrics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ingestBody)))
                .andExpect(status().isCreated());

        MvcResult metricsResult = mockMvc.perform(get("/api/projects/" + projectId + "/metrics")
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> metricPage = objectMapper.readValue(metricsResult.getResponse().getContentAsString(), Map.class);
        assertThat(metricPage.get("totalElements")).isEqualTo(1);
        List<?> metricContent = (List<?>) metricPage.get("content");
        assertThat(metricContent).hasSize(1);
        assertThat(((Map<?, ?>) metricContent.get(0)).get("metricName")).isEqualTo("cpu_usage_percent");

        mockMvc.perform(get("/api/projects/" + projectId + "/metrics")
                        .param("size", "101")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        MvcResult alertsResult = mockMvc.perform(get("/api/alerts").param("projectId", projectId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> alertPage = objectMapper.readValue(alertsResult.getResponse().getContentAsString(), Map.class);
        List<?> alerts = (List<?>) alertPage.get("content");
        assertThat(alerts).hasSize(1);
        Map<?, ?> alert = (Map<?, ?>) alerts.get(0);
        assertThat(alert.get("status")).isEqualTo("OPEN");
        assertThat(alert.get("severity")).isEqualTo("HIGH");

        Long alertId = Long.valueOf(alert.get("id").toString());
        mockMvc.perform(post("/api/alerts/" + alertId + "/ack")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // A second breaching point should NOT duplicate the alert.
        mockMvc.perform(post("/api/projects/" + projectId + "/metrics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ingestBody)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldWaitForTheWholeConfiguredWindowAndIgnoreInterruptedBreaches() throws Exception {
        String token = registerAndLogin("obs-window-owner");
        Long projectId = createProject(token);

        for (String metricName : List.of("cpu_usage_percent", "memory_usage_percent")) {
            Map<String, Object> ruleBody = Map.of(
                    "name", metricName,
                    "metric", metricName,
                    "operator", ">",
                    "threshold", 80.0,
                    "duration", "5m",
                    "severity", "high"
            );
            mockMvc.perform(post("/api/projects/" + projectId + "/alert-rules")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ruleBody)))
                    .andExpect(status().isCreated());
        }

        LocalDateTime start = LocalDateTime.now().minusMinutes(10).withNano(0);
        List<Map<String, Object>> points = List.of(
                timedPoint("cpu_usage_percent", 90.0, start),
                timedPoint("cpu_usage_percent", 91.0, start.plusMinutes(5)),
                timedPoint("memory_usage_percent", 90.0, start),
                timedPoint("memory_usage_percent", 70.0, start.plusMinutes(2)),
                timedPoint("memory_usage_percent", 91.0, start.plusMinutes(5))
        );
        mockMvc.perform(post("/api/projects/" + projectId + "/metrics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("points", points))))
                .andExpect(status().isCreated());

        MvcResult alertsResult = mockMvc.perform(get("/api/alerts")
                        .param("projectId", projectId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> alertPage = objectMapper.readValue(alertsResult.getResponse().getContentAsString(), Map.class);
        List<?> alerts = (List<?>) alertPage.get("content");
        assertThat(alerts).hasSize(1);
        assertThat(((Map<?, ?>) alerts.get(0)).get("message")).asString().contains("cpu_usage_percent");
    }

    private Map<String, Object> timedPoint(String metricName, double value, LocalDateTime capturedAt) {
        Map<String, Object> point = new HashMap<>();
        point.put("metricName", metricName);
        point.put("value", value);
        point.put("capturedAt", capturedAt);
        return point;
    }

    @Test
    void shouldIngestAndSearchLogsWithFilters() throws Exception {
        String token = registerAndLogin("obs-logs-owner");
        Long projectId = createProject(token);

        Map<String, Object> errorEntry = new HashMap<>();
        errorEntry.put("serviceName", "order-service");
        errorEntry.put("environment", "production");
        errorEntry.put("level", "ERROR");
        errorEntry.put("message", "Database connection timeout");

        Map<String, Object> infoEntry = new HashMap<>();
        infoEntry.put("serviceName", "order-service");
        infoEntry.put("environment", "production");
        infoEntry.put("level", "INFO");
        infoEntry.put("message", "Order created successfully");

        Map<String, Object> ingestBody = Map.of("entries", List.of(errorEntry, infoEntry));

        mockMvc.perform(post("/api/projects/" + projectId + "/logs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ingestBody)))
                .andExpect(status().isCreated());

        MvcResult searchResult = mockMvc.perform(get("/api/projects/" + projectId + "/logs")
                        .param("level", "ERROR")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> page = objectMapper.readValue(searchResult.getResponse().getContentAsString(), Map.class);
        List<?> content = (List<?>) page.get("content");
        assertThat(content).hasSize(1);
        Map<?, ?> onlyResult = (Map<?, ?>) content.get(0);
        assertThat(onlyResult.get("level")).isEqualTo("ERROR");
        assertThat(onlyResult.get("message")).isEqualTo("Database connection timeout");
    }
}
