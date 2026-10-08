package com.devpulse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class LogIngestRequest {

    @NotEmpty(message = "At least one log entry is required")
    @Size(max = 1000, message = "A batch can contain at most 1000 log entries")
    @Valid
    private List<Entry> entries;

    public LogIngestRequest() {}

    public List<Entry> getEntries() { return entries; }
    public void setEntries(List<Entry> entries) { this.entries = entries; }

    public static class Entry {

        // Limits mirror the column sizes in V1__init_schema.sql (logs table);
        // `message` is TEXT in the database but is capped to keep a single entry bounded.
        @Size(max = 150, message = "serviceName must be at most 150 characters")
        private String serviceName;

        @Size(max = 50, message = "environment must be at most 50 characters")
        private String environment;

        @Size(max = 50, message = "level must be at most 50 characters")
        private String level;

        @NotBlank(message = "message is required")
        @Size(max = 10000, message = "message must be at most 10000 characters")
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
