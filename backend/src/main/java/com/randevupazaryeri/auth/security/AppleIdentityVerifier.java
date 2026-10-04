package com.randevupazaryeri.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.randevupazaryeri.common.exception.UnauthorizedException;
import com.randevupazaryeri.config.AppleProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.security.Key;
import java.security.PublicKey;
import java.util.Map;
import java.util.Set;

/** Verifies Sign in with Apple identity tokens against Apple's published signing keys. */
@Slf4j
@Component
public class AppleIdentityVerifier {
    private static final String ISSUER = "https://appleid.apple.com";
    private static final URI KEYS_URI = URI.create("https://appleid.apple.com/auth/keys");

    public record AppleIdentity(String subject, String email, boolean emailVerified) { }

    private final AppleProperties properties;
    private final JwksKeyCache keys;

    public AppleIdentityVerifier(AppleProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.keys = new JwksKeyCache("Apple", KEYS_URI, objectMapper);
    }

    public AppleIdentity verify(String identityToken) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .keyLocator(new LocatorAdapter<Key>() {
                        @Override
                        protected Key locate(JwsHeader header) {
                            return keys.publicKey(header.getKeyId());
                        }
                    })
                    .requireIssuer(ISSUER)
                    .clockSkewSeconds(60)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Rejected Apple identity token: {}", ex.getMessage());
            throw new UnauthorizedException("Invalid Apple identity token");
        }
        Set<String> audience = claims.getAudience();
        if (audience == null || audience.stream().noneMatch(properties.getAudiences()::contains)) {
            log.warn("Rejected Apple identity token for audience {}", audience);
            throw new UnauthorizedException("Apple identity token was issued for another app");
        }
        Object verified = claims.get("email_verified");
        return new AppleIdentity(
                claims.getSubject(),
                claims.get("email", String.class),
                Boolean.TRUE.equals(verified) || "true".equals(verified));
    }

    /** Test seam: replaces Apple's key set without a network call. */
    void useKeys(Map<String, PublicKey> keys) {
        this.keys.useKeys(keys);
    }
}
