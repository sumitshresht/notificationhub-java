package com.notificationhub.sdk.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationhub.sdk.auth.HmacSigner;
import com.notificationhub.sdk.exception.NotificationHubException;
import com.notificationhub.sdk.model.TemplateRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

public class TemplateApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final HmacSigner signer;
    private final String baseUrl;

    public TemplateApiClient(HttpClient httpClient, ObjectMapper objectMapper, HmacSigner signer, String baseUrl) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.signer = signer;
        this.baseUrl = baseUrl;
    }

    /**
     * Creates a new Notification Template.
     */
    public Map<String, Object> create(TemplateRequest request) throws NotificationHubException {
        return executePostPut(baseUrl + "/api/v1/templates", "POST", request);
    }

    /**
     * Updates an existing Notification Template.
     */
    public Map<String, Object> update(UUID templateId, TemplateRequest request) throws NotificationHubException {
        return executePostPut(baseUrl + "/api/v1/templates/" + templateId, "PUT", request);
    }

    /**
     * Fetches a specific Template by ID.
     */
    public Map<String, Object> get(UUID templateId) throws NotificationHubException {
        return executeGetDelete(baseUrl + "/api/v1/templates/" + templateId, "GET");
    }

    /**
     * Deletes a Template by ID.
     */
    public void delete(UUID templateId) throws NotificationHubException {
        executeGetDelete(baseUrl + "/api/v1/templates/" + templateId, "DELETE");
    }

    /**
     * Previews a template by rendering the Handlebars variables against the actual template on the server.
     */
    public Map<String, String> preview(UUID templateId, Map<String, Object> variables) throws NotificationHubException {
        try {
            // Your server expects: {"variables": { ... }}
            String jsonPayload = objectMapper.writeValueAsString(Map.of("variables", variables));

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/templates/" + templateId + "/preview"))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

            HttpRequest httpRequest = signer.signRequest(requestBuilder, jsonPayload).build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<Map<String, String>>() {});
            } else {
                throw new NotificationHubException("Template preview failed", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            if (e instanceof NotificationHubException) throw (NotificationHubException) e;
            throw new NotificationHubException("Network or serialization error", 0, e.getMessage());
        }
    }

    // --- Internal Helper Methods ---

    private Map<String, Object> executePostPut(String url, String method, Object body) throws NotificationHubException {
        try {
            String jsonPayload = objectMapper.writeValueAsString(body);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method(method, HttpRequest.BodyPublishers.ofString(jsonPayload));

            HttpRequest httpRequest = signer.signRequest(requestBuilder, jsonPayload).build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
            }
            throw new NotificationHubException("API Request Failed", response.statusCode(), response.body());
        } catch (Exception e) {
            if (e instanceof NotificationHubException) throw (NotificationHubException) e;
            throw new NotificationHubException("Execution failed", 0, e.getMessage());
        }
    }

    private Map<String, Object> executeGetDelete(String url, String method) throws NotificationHubException {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method(method, HttpRequest.BodyPublishers.noBody());

            // GET and DELETE requests have no payload body to hash, so we pass an empty string
            HttpRequest httpRequest = signer.signRequest(requestBuilder, "").build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                if (method.equals("DELETE")) return Map.of("status", "success");
                return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
            }
            throw new NotificationHubException("API Request Failed", response.statusCode(), response.body());
        } catch (Exception e) {
            if (e instanceof NotificationHubException) throw (NotificationHubException) e;
            throw new NotificationHubException("Execution failed", 0, e.getMessage());
        }
    }
}