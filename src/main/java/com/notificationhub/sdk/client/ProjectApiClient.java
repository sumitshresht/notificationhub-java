package com.notificationhub.sdk.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationhub.sdk.auth.HmacSigner;
import com.notificationhub.sdk.exception.NotificationHubException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

public class ProjectApiClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final HmacSigner signer;
    private final String baseUrl;

    public ProjectApiClient(HttpClient httpClient, ObjectMapper objectMapper, HmacSigner signer, String baseUrl) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.signer = signer;
        this.baseUrl = baseUrl;
    }

    public Map<String, Object> create(String projectName) throws NotificationHubException {
        return execute(baseUrl + "/api/v1/projects", "POST", Map.of("name", projectName));
    }

    public Map<String, Object> get(UUID projectId) throws NotificationHubException {
        return execute(baseUrl + "/api/v1/projects/" + projectId, "GET", null);
    }

    // 🟢 ADDED: List Projects with Pagination
    public Map<String, Object> list(int page, int size) throws NotificationHubException {
        return execute(baseUrl + "/api/v1/projects?page=" + page + "&size=" + size, "GET", null);
    }

    // 🟢 ADDED: Update Project Status
    public Map<String, Object> updateStatus(UUID projectId, String status) throws NotificationHubException {
        return execute(baseUrl + "/api/v1/projects/" + projectId + "/status?status=" + status.toUpperCase(), "PATCH", "");
    }

    // 🟢 ADDED: Update Project Name
    public Map<String, Object> updateName(UUID projectId, String newName) throws NotificationHubException {
        return execute(baseUrl + "/api/v1/projects/" + projectId, "PUT", Map.of("name", newName));
    }

    // 🟢 ADDED: Delete Project
    public void delete(UUID projectId) throws NotificationHubException {
        execute(baseUrl + "/api/v1/projects/" + projectId, "DELETE", null);
    }

    public Map<String, Object> rotateSecret(UUID projectId) throws NotificationHubException {
        return execute(baseUrl + "/api/v1/projects/" + projectId + "/rotate-secret", "POST", "");
    }

    public Map<String, Object> rotateApiKey(UUID projectId) throws NotificationHubException {
        return execute(baseUrl + "/api/v1/projects/" + projectId + "/rotate-api-key", "POST", "");
    }

    private Map<String, Object> execute(String url, String method, Object body) throws NotificationHubException {
        try {
            String jsonPayload = body != null && !body.equals("") ? objectMapper.writeValueAsString(body) : "";
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(jsonPayload));

            HttpRequest httpRequest = signer.signRequest(requestBuilder, jsonPayload).build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                if (method.equals("DELETE")) return Map.of("status", "success");
                return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
            }
            throw new NotificationHubException("Project API Request Failed", response.statusCode(), response.body());
        } catch (Exception e) {
            if (e instanceof NotificationHubException) throw (NotificationHubException) e;
            throw new NotificationHubException("Execution failed", 0, e.getMessage());
        }
    }
}