package com.randevupazaryeri.auth.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.randevupazaryeri.config.AppleProperties;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Revokes Sign in with Apple tokens when an account is deleted, as App Store Review requires. The app
 * sends a fresh authorization code; it is exchanged for a token, which is then revoked. Best effort: a
 * failure is logged and never blocks the deletion. Codes, tokens and keys are never logged.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppleTokenRevoker {

    private static final String APPLE = "https://appleid.apple.com";

    private final AppleProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    /**
     * @param expectedSubject the deleted account's Apple user id; tokens of a different Apple ID are left alone
     */
    @Async
    public void revoke(String authorizationCode, String clientId, String expectedSubject) {
        if (authorizationCode == null || authorizationCode.isBlank()) {
            return;
        }
        if (!properties.canRevokeTokens()) {
            log.warn("Apple token revocation skipped: APPLE_SIGNIN_KEY_ID or APPLE_SIGNIN_PRIVATE_KEY is not configured");
            return;
        }
        if (clientId == null || !properties.getAudiences().contains(clientId)) {
            log.warn("Apple token revocation skipped: unknown client id");
            return;
        }
        try {
            String secret = clientSecret(clientId);
            HttpResponse<String> exchange = post("/auth/token", Map.of(
                    "grant_type", "authorization_code",
                    "code", authorizationCode,
                    "client_id", clientId,
                    "client_secret", secret));
            JsonNode tokens = objectMapper.readTree(exchange.body());
            if (exchange.statusCode() != 200) {
                log.warn("Apple token exchange failed ({}): {}", exchange.statusCode(), tokens.path("error").asText("unknown"));
                return;
            }
            String subject = subjectOf(tokens.path("id_token").asText(""));
            if (expectedSubject != null && !expectedSubject.equals(subject)) {
                log.warn("Apple token revocation skipped: the code belongs to a different Apple ID");
                return;
            }
            boolean hasRefresh = !tokens.path("refresh_token").asText("").isBlank();
            HttpResponse<String> revoke = post("/auth/revoke", Map.of(
                    "client_id", clientId,
                    "client_secret", secret,
                    "token", hasRefresh ? tokens.path("refresh_token").asText() : tokens.path("access_token").asText(),
                    "token_type_hint", hasRefresh ? "refresh_token" : "access_token"));
            if (revoke.statusCode() == 200) {
                log.info("Apple tokens revoked for a deleted account");
            } else {
                log.warn("Apple token revocation failed ({})", revoke.statusCode());
            }
        } catch (IOException | GeneralSecurityException | IllegalArgumentException ex) {
            log.warn("Apple token revocation failed: {}", ex.getClass().getSimpleName());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private HttpResponse<String> post(String path, Map<String, String> form) throws IOException, InterruptedException {
        String body = form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder(URI.create(APPLE + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** Short-lived ES256 client secret, as described in Apple's "Generate and validate tokens". */
    String clientSecret(String clientId) throws GeneralSecurityException {
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId(properties.getSignInKeyId().trim()).and()
                .issuer(properties.getTeamId().trim())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(5))))
                .audience().add(APPLE).and()
                .subject(clientId)
                .signWith(privateKey(), Jwts.SIG.ES256)
                .compact();
    }

    private PrivateKey privateKey() throws GeneralSecurityException {
        String base64 = properties.getSignInPrivateKey()
                .replace("\\n", "\n")
                .replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }

    /** The id_token comes straight from Apple over TLS, so only its payload is read. */
    private String subjectOf(String idToken) throws IOException {
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
        return payload.path("sub").asText(null);
    }
}
