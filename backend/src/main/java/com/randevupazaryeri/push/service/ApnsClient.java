package com.randevupazaryeri.push.service;

import com.randevupazaryeri.config.ApnsProperties;
import com.randevupazaryeri.push.entity.PushApp;
import com.randevupazaryeri.push.entity.PushEnvironment;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Minimal APNs HTTP/2 client using token-based (.p8) authentication. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApnsClient {
    /** Apple rejects provider tokens older than 60 minutes and throttles refreshes faster than 20. */
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(45);

    public enum Result { DELIVERED, INVALID_TOKEN, FAILED }

    private record ProviderToken(String value, Instant issuedAt) { }

    private final ApnsProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final Map<String, PrivateKey> signingKeys = new ConcurrentHashMap<>();
    private final Map<String, ProviderToken> providerTokens = new ConcurrentHashMap<>();

    public boolean isConfigured(PushApp app) {
        return properties.keyFor(app) != null;
    }

    public Result send(String deviceToken, PushApp app, PushEnvironment environment, String payloadJson) {
        ApnsProperties.Key key = properties.keyFor(app);
        if (key == null) {
            return Result.FAILED;
        }
        String host = environment == PushEnvironment.PRODUCTION ? "api.push.apple.com" : "api.sandbox.push.apple.com";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://" + host + "/3/device/" + deviceToken))
                    .timeout(Duration.ofSeconds(15))
                    .header("authorization", "bearer " + providerToken(key))
                    .header("apns-topic", properties.topicFor(app))
                    .header("apns-push-type", "alert")
                    .header("apns-priority", "10")
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return Result.DELIVERED;
            }
            String body = response.body();
            if (response.statusCode() == 410 || body.contains("BadDeviceToken") || body.contains("DeviceTokenNotForTopic")) {
                return Result.INVALID_TOKEN;
            }
            if (response.statusCode() == 403 && body.contains("ExpiredProviderToken")) {
                providerTokens.remove(key.getKeyId());
            }
            log.warn("APNs push failed ({}): {}", response.statusCode(), body);
            return Result.FAILED;
        } catch (IOException | GeneralSecurityException | IllegalArgumentException ex) {
            log.warn("APNs push failed: {}", ex.getMessage());
            return Result.FAILED;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Result.FAILED;
        }
    }

    synchronized String providerToken(ApnsProperties.Key key) throws GeneralSecurityException {
        Instant now = Instant.now();
        ProviderToken cached = providerTokens.get(key.getKeyId());
        if (cached != null && cached.issuedAt().plus(TOKEN_LIFETIME).isAfter(now)) {
            return cached.value();
        }
        String token = Jwts.builder()
                .header().keyId(key.getKeyId()).and()
                .issuer(properties.getTeamId())
                .issuedAt(Date.from(now))
                .signWith(signingKey(key), Jwts.SIG.ES256)
                .compact();
        providerTokens.put(key.getKeyId(), new ProviderToken(token, now));
        return token;
    }

    private PrivateKey signingKey(ApnsProperties.Key key) throws GeneralSecurityException {
        PrivateKey cached = signingKeys.get(key.getKeyId());
        if (cached != null) {
            return cached;
        }
        String base64 = key.getPrivateKey()
                .replace("\\n", "\n")
                .replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        PrivateKey parsed = KeyFactory.getInstance("EC")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        signingKeys.put(key.getKeyId(), parsed);
        return parsed;
    }
}
