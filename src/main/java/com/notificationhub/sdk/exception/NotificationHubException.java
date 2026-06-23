package com.notificationhub.sdk.exception;

public class NotificationHubException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;

    public NotificationHubException(String message, int statusCode, String responseBody) {
        super(message + " (HTTP " + statusCode + "): " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}