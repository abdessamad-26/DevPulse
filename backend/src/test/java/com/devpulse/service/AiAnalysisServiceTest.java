package com.devpulse.service;

import com.devpulse.dto.IncidentAnalysisRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AiAnalysisServiceTest {

    @Test
    void sendsIncidentAsJsonAndMapsAnalysisResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        String responseBody = """
                {
                  "classification": "DATABASE",
                  "confidence": 0.92,
                  "rootCause": "The database connection pool is exhausted.",
                  "recommendations": ["Increase pool capacity."],
                  "signals": ["pool timeout"]
                }
                """;
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/analysis/incidents", exchange -> {
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            AiAnalysisService service = new AiAnalysisService(
                    RestClient.builder(),
                    objectMapper,
                    "http://127.0.0.1:" + server.getAddress().getPort());

            var result = service.analyzeIncident(new IncidentAnalysisRequest(
                    "Checkout errors",
                    "The connection pool is exhausted.",
                    "HIGH",
                    "checkout-api",
                    List.of("pool timeout")));

            assertThat(contentType.get()).startsWith("application/json");
            var sent = objectMapper.readTree(requestBody.get());
            assertThat(sent.path("title").asText()).isEqualTo("Checkout errors");
            assertThat(sent.path("description").asText()).isEqualTo("The connection pool is exhausted.");
            assertThat(sent.path("recent_logs").get(0).asText()).isEqualTo("pool timeout");
            assertThat(result.rootCause()).isEqualTo("The database connection pool is exhausted.");
        } finally {
            server.stop(0);
        }
    }
}
