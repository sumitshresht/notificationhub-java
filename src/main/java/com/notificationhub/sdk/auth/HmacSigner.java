package com.notificationhub.sdk.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

public class HmacSigner {

    private static final String HMAC_ALGO = "HmacSHA256";
    private final String apiKey;
    private final String rawSecret;

    public HmacSigner(String apiKey, String rawSecret) {
        this.apiKey = apiKey;
        this.rawSecret = rawSecret;
    }

    /**
     * Injects the required authentication and HMAC headers into the HTTP request.
     */
    public HttpRequest.Builder signRequest(HttpRequest.Builder requestBuilder, String payload) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = generateSignature(timestamp, payload);

        return requestBuilder
                .header("X-API-Key", apiKey)
                .header("Authorization", "Bearer " + rawSecret)
                .header("X-Timestamp", timestamp)
                .header("X-Signature", signature)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }

    private String generateSignature(String timestamp, String payload) {
        try {
            String dataToSign = timestamp + "." + (payload == null ? "" : payload);
            
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKeySpec = new SecretKeySpec(rawSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
            mac.init(secretKeySpec);
            
            byte[] hmacBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }
}