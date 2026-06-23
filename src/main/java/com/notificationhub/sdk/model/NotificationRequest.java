package com.notificationhub.sdk.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationRequest {
    private final List<String> channels;
    private final Map<String, String> to;
    private final String templateId;
    private final String subject;
    private final String message;
    private final Map<String, Object> vars;
    private final Instant scheduledFor;

    private NotificationRequest(Builder builder) {
        this.channels = builder.channels;
        this.to = builder.to;
        this.templateId = builder.templateId;
        this.subject = builder.subject;
        this.message = builder.message;
        this.vars = builder.vars;
        this.scheduledFor = builder.scheduledFor;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters omitted for brevity...

    public static class Builder {
        private final List<String> channels = new ArrayList<>();
        private final Map<String, String> to = new HashMap<>();
        private String templateId;
        private String subject;
        private String message;
        private final Map<String, Object> vars = new HashMap<>();
        private Instant scheduledFor;

        public Builder addChannel(String channel) {
            this.channels.add(channel.toUpperCase());
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

        public Builder toPushToken(String token) {
            this.to.put("token", token);
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

        public Builder templateId(String templateId) {
            this.templateId = templateId;
            return this;
        }

        public Builder addVariable(String key, Object value) {
            this.vars.put(key, value);
            return this;
        }

        public Builder scheduleFor(Instant instant) {
            this.scheduledFor = instant;
            return this;
        }

        public NotificationRequest build() {
            if (channels.isEmpty()) {
                throw new IllegalStateException("At least one channel must be specified.");
            }
            return new NotificationRequest(this);
        }
    }
}