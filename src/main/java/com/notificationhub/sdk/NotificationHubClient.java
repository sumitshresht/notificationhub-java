package com.notificationhub.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.notificationhub.sdk.auth.HmacSigner;
import com.notificationhub.sdk.client.*;

import java.net.http.HttpClient;
import java.time.Duration;

public class NotificationHubClient {

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

    public NotificationHubClient(String apiKey, String rawSecret, String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.signer = new HmacSigner(apiKey, rawSecret);

        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        this.notifications = new NotificationApiClient(httpClient, objectMapper, signer, this.baseUrl);
        this.templates = new TemplateApiClient(httpClient, objectMapper, signer, this.baseUrl);
        this.analytics = new AnalyticsApiClient(httpClient, objectMapper, signer, this.baseUrl);
        this.projects = new ProjectApiClient(httpClient, objectMapper, signer, this.baseUrl);
        this.providers = new ProviderApiClient(httpClient, objectMapper, signer, this.baseUrl);
    }

    public NotificationApiClient notifications() { return notifications; }
    public TemplateApiClient templates() { return templates; }
    public AnalyticsApiClient analytics() { return analytics; }
    public ProjectApiClient projects() { return projects; }
    public ProviderApiClient providers() { return providers; }
}