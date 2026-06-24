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

    public Map<String, Object> create(TemplateRequest request) throws NotificationHubException {
        return executePostPut(baseUrl + "/api/v1/templates", "POST", request);
    }

    public Map<String, Object> update(UUID templateId, TemplateRequest request) throws NotificationHubException {
        return executePostPut(baseUrl + "/api/v1/templates/" + templateId, "PUT", request);
    }

    public Map<String, Object> get(UUID templateId) throws NotificationHubException {
        return executeGetDelete(baseUrl + "/api/v1/templates/" + templateId, "GET");
    }

    // 🟢 ADDED: List templates with Spring Pagination
    public Map<String, Object> list(int page, int size) throws NotificationHubException {
        return executeGetDelete(baseUrl + "/api/v1/templates?page=" + page + "&size=" + size, "GET");
    }

    public void delete(UUID templateId) throws NotificationHubException {
        executeGetDelete(baseUrl + "/api/v1/templates/" + templateId, "DELETE");
    }

    public Map<String, String> preview(UUID templateId, Map<String, Object> variables) throws NotificationHubException {
        return executePreview(baseUrl + "/api/v1/templates/" + templateId + "/preview", Map.of("variables", variables));
    }

    // 🟢 ADDED: Render raw HTML strings through the Engine without saving them first
    public Map<String, String> previewRaw(String rawTemplateHtml, Map<String, Object> variables) throws NotificationHubException {
        return executePreview(baseUrl + "/api/v1/templates/preview", Map.of(
                "rawTemplateHtml", rawTemplateHtml,
                "variables", variables
        ));
    }

    private Map<String, String> executePreview(String url, Map<String, Object> payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

            HttpRequest httpRequest = signer.signRequest(requestBuilder, jsonPayload).build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<Map<String, String>>() {});
            }
            throw new NotificationHubException("Template preview failed", response.statusCode(), response.body());
        } catch (Exception e) {
            if (e instanceof NotificationHubException) throw (NotificationHubException) e;
            throw new NotificationHubException("Network or serialization error", 0, e.getMessage());
        }
    }

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