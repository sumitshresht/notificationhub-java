package com.notificationhub.sdk.model;

public class TemplateRequest {
    private final String name;
    private final String subject;
    private final String content;

    private TemplateRequest(Builder builder) {
        this.name = builder.name;
        this.subject = builder.subject;
        this.content = builder.content;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters...
    public String getName() { return name; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }

    public static class Builder {
        private String name;
        private String subject;
        private String content;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public TemplateRequest build() {
            if (name == null || subject == null || content == null) {
                throw new IllegalStateException("Name, subject, and content are required.");
            }
            return new TemplateRequest(this);
        }
    }
}