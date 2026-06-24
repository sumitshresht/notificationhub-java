package com.notificationhub.sdk.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationhub.sdk.auth.HmacSigner;
import com.notificationhub.sdk.exception.NotificationHubException;
import com.notificationhub.sdk.model.NotificationRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NotificationApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final HmacSigner signer;
    private final String baseUrl;

    public NotificationApiClient(HttpClient httpClient, ObjectMapper objectMapper, HmacSigner signer, String baseUrl) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.signer = signer;
        this.baseUrl = baseUrl;
    }

    public Map<String, Object> send(NotificationRequest request) throws NotificationHubException {
        return sendWithIdempotency(request, null);
    }

    public Map<String, Object> sendWithIdempotency(NotificationRequest request, UUID idempotencyKey) throws NotificationHubException {
        return executePost(baseUrl + "/api/v1/notifications", request, idempotencyKey, new TypeReference<Map<String, Object>>() {});
    }

    // 🟢 ADDED: Bulk Dispatching
    public Map<String, Object> sendBulk(List<NotificationRequest> requests) throws NotificationHubException {
        return executePost(baseUrl + "/api/v1/notifications/bulk", requests, null, new TypeReference<Map<String, Object>>() {});
    }

    // 🟢 ADDED: Search/List Notifications
    public List<Map<String, Object>> search(String channel, String status, int limit, int offset) throws NotificationHubException {
        StringBuilder url = new StringBuilder(baseUrl).append("/api/v1/notifications?limit=").append(limit).append("&offset=").append(offset);
        if (channel != null) url.append("&channel=").append(channel);
        if (status != null) url.append("&status=").append(status);
        return executeGet(url.toString(), new TypeReference<List<Map<String, Object>>>() {});
    }

    // 🟢 ADDED: Get Single Notification
    public Map<String, Object> get(UUID notificationId) throws NotificationHubException {
        return executeGet(baseUrl + "/api/v1/notifications/" + notificationId, new TypeReference<Map<String, Object>>() {});
    }

    // 🟢 ADDED: Get Notification Receipts & Metrics
    public Map<String, Object> getReceipts(UUID notificationId) throws NotificationHubException {
        return executeGet(baseUrl + "/api/v1/notifications/" + notificationId + "/receipts", new TypeReference<Map<String, Object>>() {});
    }

    // 🟢 ADDED: Cancel Scheduled Task
    public Map<String, String> cancelSchedule(UUID taskId) throws NotificationHubException {
        return executeDelete(baseUrl + "/api/v1/notifications/schedule/" + taskId, new TypeReference<Map<String, String>>() {});
    }

    // 🟢 ADDED: Tracking Endpoints (Bypasses HMAC Auth intentionally)
    public byte[] trackOpen(UUID notificationId) throws NotificationHubException {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/track/open/" + notificationId)).GET().build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) return response.body();
            throw new NotificationHubException("Tracking Pixel Failed", response.statusCode(), new String(response.body()));
        } catch (Exception e) {
            throw new NotificationHubException("Network error", 0, e.getMessage());
        }
    }

    // --- Internal Helpers ---

    private <T> T executePost(String url, Object body, UUID idempotencyKey, TypeReference<T> typeRef) throws NotificationHubException {
        try {
            String jsonPayload = objectMapper.writeValueAsString(body);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

            if (idempotencyKey != null) {
                requestBuilder.header("Idempotency-Key", idempotencyKey.toString());
            }

            HttpRequest httpRequest = signer.signRequest(requestBuilder, jsonPayload).build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), typeRef);
            }
            throw new NotificationHubException("API Request Failed", response.statusCode(), response.body());
        } catch (Exception e) {
            if (e instanceof NotificationHubException) throw (NotificationHubException) e;
            throw new NotificationHubException("Network or serialization error occurred", 0, e.getMessage());
        }
    }

    private <T> T executeGet(String url, TypeReference<T> responseType) throws NotificationHubException {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
            HttpRequest httpRequest = signer.signRequest(requestBuilder, "").build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), responseType);
            }
            throw new NotificationHubException("API Request Failed", response.statusCode(), response.body());
        } catch (Exception e) {
            if (e instanceof NotificationHubException) throw (NotificationHubException) e;
            throw new NotificationHubException("Execution failed", 0, e.getMessage());
        }
    }

    private <T> T executeDelete(String url, TypeReference<T> responseType) throws NotificationHubException {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(url)).DELETE();
            HttpRequest httpRequest = signer.signRequest(requestBuilder, "").build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), responseType);
            }
            throw new NotificationHubException("API Request Failed", response.statusCode(), response.body());
        } catch (Exception e) {
            if (e instanceof NotificationHubException) throw (NotificationHubException) e;
            throw new NotificationHubException("Execution failed", 0, e.getMessage());
        }
    }
}