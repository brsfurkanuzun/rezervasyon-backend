package com.randevupazaryeri.auth.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.randevupazaryeri.common.exception.UnauthorizedException;
import com.randevupazaryeri.config.AppleProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Verifies Sign in with Apple identity tokens against Apple's published signing keys. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppleIdentityVerifier {
    private static final String ISSUER = "https://appleid.apple.com";
    private static final URI KEYS_URI = URI.create("https://appleid.apple.com/auth/keys");
    private static final Duration KEYS_MAX_AGE = Duration.ofHours(24);
    private static final Duration MIN_REFETCH_INTERVAL = Duration.ofMinutes(5);

    public record AppleIdentity(String subject, String email, boolean emailVerified) { }

    private final AppleProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private Map<String, PublicKey> keys = Map.of();
    private Instant keysFetchedAt = Instant.EPOCH;

    public AppleIdentity verify(String identityToken) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .keyLocator(new LocatorAdapter<Key>() {
                        @Override
                        protected Key locate(JwsHeader header) {
                            return publicKey(header.getKeyId());
                        }
                    })
                    .requireIssuer(ISSUER)
                    .clockSkewSeconds(60)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid Apple identity token");
        }
        Set<String> audience = claims.getAudience();
        if (audience == null || audience.stream().noneMatch(properties.getAudiences()::contains)) {
            throw new UnauthorizedException("Apple identity token was issued for another app");
        }
        Object verified = claims.get("email_verified");
        return new AppleIdentity(
                claims.getSubject(),
                claims.get("email", String.class),
                Boolean.TRUE.equals(verified) || "true".equals(verified));
    }

    private synchronized PublicKey publicKey(String keyId) {
        Instant now = Instant.now();
        PublicKey key = keys.get(keyId);
        boolean stale = keysFetchedAt.plus(KEYS_MAX_AGE).isBefore(now);
        boolean mayRefetch = keysFetchedAt.plus(MIN_REFETCH_INTERVAL).isBefore(now);
        if ((key == null || stale) && mayRefetch) {
            keys = fetchKeys();
            keysFetchedAt = now;
            key = keys.get(keyId);
        }
        if (key == null) {
            throw new UnauthorizedException("Unknown Apple signing key");
        }
        return key;
    }

    /** Test seam: replaces Apple's key set without a network call. */
    synchronized void useKeys(Map<String, PublicKey> keys) {
        this.keys = Map.copyOf(keys);
        this.keysFetchedAt = Instant.now();
    }

    private Map<String, PublicKey> fetchKeys() {
        try {
            HttpRequest request = HttpRequest.newBuilder(KEYS_URI).timeout(Duration.ofSeconds(10)).GET().build();
            String body = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
            Map<String, PublicKey> result = new HashMap<>();
            KeyFactory factory = KeyFactory.getInstance("RSA");
            Base64.Decoder decoder = Base64.getUrlDecoder();
            for (JsonNode jwk : objectMapper.readTree(body).path("keys")) {
                if (!"RSA".equals(jwk.path("kty").asText())) {
                    continue;
                }
                BigInteger modulus = new BigInteger(1, decoder.decode(jwk.path("n").asText()));
                BigInteger exponent = new BigInteger(1, decoder.decode(jwk.path("e").asText()));
                result.put(jwk.path("kid").asText(), factory.generatePublic(new RSAPublicKeySpec(modulus, exponent)));
            }
            return result;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return keys;
        } catch (Exception ex) {
            log.warn("Could not fetch Apple signing keys: {}", ex.getMessage());
            return keys;
        }
    }
}
