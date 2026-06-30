package com.notificationhub.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.notificationhub.sdk.auth.HmacSigner;
import com.notificationhub.sdk.client.*;

import java.net.http.HttpClient;
import java.time.Duration;

public class NotificationHubClient {

    // The integrated production URL defaults here.
    private static final String DEFAULT_BASE_URL = "https://api.notification-server.dev-space.dev";

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final HmacSigner signer;

    // Sub-clients
    private final NotificationApiClient notifications;
    private final TemplateApiClient templates;
    private final AnalyticsApiClient analytics;
    private final ProjectApiClient projects;
    private final ProviderApiClient providers;

    // Private constructor forces users to use the Builder
    private NotificationHubClient(Builder builder) {
        if (builder.apiKey == null || builder.apiKey.isBlank()) {
            throw new IllegalArgumentException("API Key is required.");
        }
        if (builder.apiSecret == null || builder.apiSecret.isBlank()) {
            throw new IllegalArgumentException("API Secret is required for HMAC signatures.");
        }

        this.baseUrl = builder.baseUrl != null ? builder.baseUrl : DEFAULT_BASE_URL;
        this.signer = new HmacSigner(builder.apiKey, builder.apiSecret);

        // Initialize shared HTTP Client and JSON Mapper
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());

        // Initialize the sub-clients, passing the shared instances down
        this.notifications = new NotificationApiClient(httpClient, objectMapper, signer, this.baseUrl);
        this.templates = new TemplateApiClient(httpClient, objectMapper, signer, this.baseUrl);
        this.analytics = new AnalyticsApiClient(httpClient, objectMapper, signer, this.baseUrl);
        this.projects = new ProjectApiClient(httpClient, objectMapper, signer, this.baseUrl);
        this.providers = new ProviderApiClient(httpClient, objectMapper, signer, this.baseUrl);
    }

    // --- Getters for the sub-clients ---
    public NotificationApiClient notifications() { return notifications; }
    public TemplateApiClient templates() { return templates; }
    public AnalyticsApiClient analytics() { return analytics; }
    public ProjectApiClient projects() { return projects; }
    public ProviderApiClient providers() { return providers; }

    // --- The Builder ---
    public static class Builder {
        private String apiKey;
        private String apiSecret;
        private String baseUrl;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder apiSecret(String apiSecret) {
            this.apiSecret = apiSecret;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            // Basic security check to ensure developers don't accidentally use HTTP in production
            if (baseUrl != null && baseUrl.startsWith("http://") && !baseUrl.contains("localhost")) {
                throw new IllegalArgumentException("Base URL must use HTTPS for production environments.");
            }
            // Strip trailing slashes to prevent // in routes later
            this.baseUrl = baseUrl != null && baseUrl.endsWith("/") ?
                    baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return this;
        }

        public NotificationHubClient build() {
            return new NotificationHubClient(this);
        }
    }
}