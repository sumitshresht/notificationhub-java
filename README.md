# Notification Hub Java SDK

The official, zero-dependency Java SDK for Notification Hub.
Send highly scalable, multi-channel notifications (Email, SMS, Push, Webhook, In-App) securely from your Java backend.

## Features
* 🔒 **Automatic Cryptographic Signing:** Handles HMAC-SHA256 signature generation seamlessly.
* 🚀 **Zero Heavy Dependencies:** Built on native `java.net.http.HttpClient` (Requires Java 17+).
* 🧱 **Fluent Builders:** Strongly typed request builders to prevent API errors.

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.notificationhub</groupId>
    <artifactId>notificationhub-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>