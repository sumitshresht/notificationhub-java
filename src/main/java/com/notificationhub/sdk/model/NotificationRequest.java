package com.notificationhub.sdk.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationRequest {
    private final List<String> channels;
    private final Map<String, String> to;       // Maps perfectly to Server's 'to'
    private final String templateId;
    private final String subject;
    private final String message;
    private final Map<String, Object> vars;     // Maps perfectly to Server's 'vars'
    private final Instant scheduledFor;         // Maps perfectly to Server's 'scheduledFor'

    private NotificationRequest(Builder builder) {
        this.channels = List.copyOf(builder.channels);
        this.to = Map.copyOf(builder.to);
        this.templateId = builder.templateId;
        this.subject = builder.subject;
        this.message = builder.message;
        this.vars = Map.copyOf(builder.vars);
        this.scheduledFor = builder.scheduledFor;
    }

    public static Builder builder() {
        return new Builder();
    }

    // --- Public Getters for Jackson Serialization ---
    public List<String> getChannels() { return channels; }
    public Map<String, String> getTo() { return to; }
    public String getTemplateId() { return templateId; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public Map<String, Object> getVars() { return vars; }
    public Instant getScheduledFor() { return scheduledFor; }

    // --- Fluent Builder Implementation ---
    public static class Builder {
        private final List<String> channels = new ArrayList<>();
        private final Map<String, String> to = new HashMap<>();
        private String templateId;
        private String subject;
        private String message;
        private final Map<String, Object> vars = new HashMap<>();
        private Instant scheduledFor;

        public Builder addChannel(String channel) {
            if (channel != null && !channel.isBlank()) {
                this.channels.add(channel.toUpperCase());
            }
            return this;
        }

        public Builder toEmail(String email) {
            this.to.put("email", email);
            return this;
        }

        public Builder toPhone(String phone) {
            this.to.put("phone", phone);
            return this;
        }

        public Builder toWebhook(String url) {
            this.to.put("url", url);
            return this;
        }

        public Builder templateId(String templateId) {
            this.templateId = templateId;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder addVariable(String key, Object value) {
            if (key != null) {
                this.vars.put(key, value);
            }
            return this;
        }

        public Builder scheduleFor(Instant scheduledFor) {
            this.scheduledFor = scheduledFor;
            return this;
        }

        public NotificationRequest build() {
            if (channels.isEmpty()) {
                throw new IllegalStateException("At least one dispatch channel must be specified.");
            }
            return new NotificationRequest(this);
        }
    }
}