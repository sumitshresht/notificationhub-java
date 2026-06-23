package com.notificationhub.sdk.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationhub.sdk.auth.HmacSigner;
import com.notificationhub.sdk.exception.NotificationHubException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AnalyticsApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final HmacSigner signer;
    private final String baseUrl;

    public AnalyticsApiClient(HttpClient httpClient, ObjectMapper objectMapper, HmacSigner signer, String baseUrl) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.signer = signer;
        this.baseUrl = baseUrl;
    }

    /**
     * Fetches real-time delivery and open rate metrics for the current project.
     */
    public Map<String, Object> getMetrics() throws NotificationHubException {
        return executeGet(baseUrl + "/api/v1/analytics", new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Fetches the system audit logs with pagination.
     */
    public List<Map<String, Object>> getAuditLogs(int limit, int offset) throws NotificationHubException {
        String url = String.format("%s/api/v1/audit?limit=%d&offset=%d", baseUrl, limit, offset);
        return executeGet(url, new TypeReference<List<Map<String, Object>>>() {});
    }

    /**
     * Fetches the Dead Letter Queue (failed notifications) with pagination.
     */
    public List<Map<String, Object>> getDeadLetterQueue(int limit, int offset) throws NotificationHubException {
        String url = String.format("%s/api/v1/dlq?limit=%d&offset=%d", baseUrl, limit, offset);
        return executeGet(url, new TypeReference<List<Map<String, Object>>>() {});
    }

    /**
     * Injects a failed notification back into the engine for retry processing.
     */
    public Map<String, String> retryFailedNotification(UUID dlqId) throws NotificationHubException {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/dlq/" + dlqId + "/retry"))
                    .POST(HttpRequest.BodyPublishers.noBody()); // POST with no body

            HttpRequest httpRequest = signer.signRequest(requestBuilder, "").build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), new TypeReference<Map<String, String>>() {});
            }
            throw new NotificationHubException("DLQ Retry Failed", response.statusCode(), response.body());
        } catch (Exception e) {
            if (e instanceof NotificationHubException) throw (NotificationHubException) e;
            throw new NotificationHubException("Execution failed", 0, e.getMessage());
        }
    }

    // --- Internal Helper Method ---

    private <T> T executeGet(String url, TypeReference<T> responseType) throws NotificationHubException {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET();

            // GET requests have no payload body to hash, so we pass an empty string
            HttpRequest httpRequest = signer.signRequest(requestBuilder, "").build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), responseType);
            }
            throw new NotificationHubException("Analytics API Request Failed", response.statusCode(), response.body());
        } catch (Exception e) {
            if (e instanceof NotificationHubException) throw (NotificationHubException) e;
            throw new NotificationHubException("Execution failed", 0, e.getMessage());
        }
    }
}