package com.randevupazaryeri.auth.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.randevupazaryeri.common.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/** RSA signing keys of an identity provider's JWKS endpoint, cached and refetched when a key rotates. */
@Slf4j
class JwksKeyCache {
    private static final Duration KEYS_MAX_AGE = Duration.ofHours(24);
    private static final Duration MIN_REFETCH_INTERVAL = Duration.ofMinutes(5);
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private final String provider;
    private final URI keysUri;
    private final ObjectMapper objectMapper;

    private Map<String, PublicKey> keys = Map.of();
    private Instant keysFetchedAt = Instant.EPOCH;

    JwksKeyCache(String provider, URI keysUri, ObjectMapper objectMapper) {
        this.provider = provider;
        this.keysUri = keysUri;
        this.objectMapper = objectMapper;
    }

    synchronized PublicKey publicKey(String keyId) {
        Instant now = Instant.now();
        PublicKey key = keys.get(keyId);
        boolean stale = keysFetchedAt.plus(KEYS_MAX_AGE).isBefore(now);
        boolean mayRefetch = keysFetchedAt.plus(MIN_REFETCH_INTERVAL).isBefore(now);
        if ((key == null || stale) && mayRefetch) {
            Map<String, PublicKey> fetched = fetchKeys();
            if (!fetched.isEmpty()) {
                keys = fetched;
                keysFetchedAt = now;
            }
            key = keys.get(keyId);
        }
        if (key == null) {
            throw new UnauthorizedException("Unknown " + provider + " signing key");
        }
        return key;
    }

    synchronized void useKeys(Map<String, PublicKey> keys) {
        this.keys = Map.copyOf(keys);
        this.keysFetchedAt = Instant.now();
    }

    private Map<String, PublicKey> fetchKeys() {
        try {
            HttpRequest request = HttpRequest.newBuilder(keysUri).timeout(Duration.ofSeconds(10)).GET().build();
            String body = HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body();
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
            return Map.of();
        } catch (Exception ex) {
            log.warn("Could not fetch {} signing keys: {}", provider, ex.getMessage());
            return Map.of();
        }
    }
}
