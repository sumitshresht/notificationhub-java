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

public class ProviderApiClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final HmacSigner signer;
    private final String baseUrl;

    public ProviderApiClient(HttpClient httpClient, ObjectMapper objectMapper, HmacSigner signer, String baseUrl) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.signer = signer;
        this.baseUrl = baseUrl;
    }

    public Map<String, String> configure(String channelType, String providerName, Map<String, String> config) throws NotificationHubException {
        Map<String, Object> payload = Map.of(
                "channelType", channelType.toUpperCase(),
                "providerName", providerName.toUpperCase(),
                "config", config
        );
        return execute(baseUrl + "/api/v1/providers/configure", "POST", payload, new TypeReference<Map<String, String>>() {});
    }

    public Map<String, Object> testConnection(String channelType, Map<String, String> config) throws NotificationHubException {
        Map<String, Object> payload = Map.of(
                "channelType", channelType.toUpperCase(),
                "config", config
        );
        return execute(baseUrl + "/api/v1/providers/test", "POST", payload, new TypeReference<Map<String, Object>>() {});
    }

    public List<Map<String, Object>> list() throws NotificationHubException {
        return execute(baseUrl + "/api/v1/providers", "GET", null, new TypeReference<List<Map<String, Object>>>() {});
    }

    private <T> T execute(String url, String method, Object body, TypeReference<T> typeRef) throws NotificationHubException {
        try {
            String jsonPayload = body != null ? objectMapper.writeValueAsString(body) : "";
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(jsonPayload));

            HttpRequest httpRequest = signer.signRequest(requestBuilder, jsonPayload).build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), typeRef);
            }
            throw new NotificationHubException("Provider API Request Failed", response.statusCode(), response.body());
        } catch (Exception e) {
            if (e instanceof NotificationHubException) throw (NotificationHubException) e;
            throw new NotificationHubException("Execution failed", 0, e.getMessage());
        }
    }
}