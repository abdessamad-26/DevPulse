package com.devpulse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

public class LogIngestRequest {

    @NotEmpty(message = "At least one log entry is required")
    @Valid
    private List<Entry> entries;

    public LogIngestRequest() {}

    public List<Entry> getEntries() { return entries; }
    public void setEntries(List<Entry> entries) { this.entries = entries; }

    public static class Entry {

        private String serviceName;
        private String environment;
        private String level;

        @NotBlank(message = "message is required")
        private String message;

        private LocalDateTime timestamp;

        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }

        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
