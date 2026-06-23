package com.notificationhub.sdk.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationhub.sdk.auth.HmacSigner;
import com.notificationhub.sdk.exception.NotificationHubException;
import com.notificationhub.sdk.model.NotificationRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

    /**
     * Dispatches a unified notification payload to the server.
     */
    public Map<String, Object> send(NotificationRequest request) throws NotificationHubException {
        return sendWithIdempotency(request, null);
    }

    /**
     * Dispatches a notification with an Idempotency Key to prevent duplicate sends on network retries.
     */
    public Map<String, Object> sendWithIdempotency(NotificationRequest request, UUID idempotencyKey) throws NotificationHubException {
        try {
            String jsonPayload = objectMapper.writeValueAsString(request);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/notifications"))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

            if (idempotencyKey != null) {
                requestBuilder.header("Idempotency-Key", idempotencyKey.toString());
            }

            // The Signer injects the X-Timestamp and X-Signature seamlessly
            HttpRequest httpRequest = signer.signRequest(requestBuilder, jsonPayload).build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), Map.class);
            } else {
                throw new NotificationHubException("API Request Failed", response.statusCode(), response.body());
            }

        } catch (Exception e) {
            if (e instanceof NotificationHubException) throw (NotificationHubException) e;
            throw new NotificationHubException("Network or serialization error occurred", 0, e.getMessage());
        }
    }
}