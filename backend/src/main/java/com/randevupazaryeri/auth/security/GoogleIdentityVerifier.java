package com.randevupazaryeri.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.randevupazaryeri.common.exception.UnauthorizedException;
import com.randevupazaryeri.config.GoogleProperties;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Verifies Google Sign-In ID tokens against Google's published signing keys. */
@Slf4j
@Component
public class GoogleIdentityVerifier {
    private static final Set<String> ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
    private static final URI KEYS_URI = URI.create("https://www.googleapis.com/oauth2/v3/certs");

    /**
     * @param hostedDomain Google Workspace domain ("hd" claim); null for consumer accounts.
     */
    public record GoogleIdentity(String subject, String email, boolean emailVerified, String hostedDomain,
                                 String firstName, String lastName) {

        /**
         * Google owns gmail.com and Workspace addresses, so only for those does a verified email prove
         * the person controls the mailbox and may take over an existing account with that address.
         */
        public boolean emailIsAuthoritative() {
            return emailVerified && email != null
                    && (hostedDomain != null || email.toLowerCase().endsWith("@gmail.com"));
        }
    }

    private final GoogleProperties properties;
    private final JwksKeyCache keys;

    public GoogleIdentityVerifier(GoogleProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.keys = new JwksKeyCache("Google", KEYS_URI, objectMapper);
    }

    public GoogleIdentity verify(String idToken) {
        List<String> audiences = properties.audiences();
        if (audiences.isEmpty()) {
            log.warn("Google sign-in attempted but no Google client id is configured");
            throw new UnauthorizedException("Google sign-in is not available");
        }
        Claims claims;
        try {
            claims = Jwts.parser()
                    .keyLocator(new LocatorAdapter<Key>() {
                        @Override
                        protected Key locate(JwsHeader header) {
                            return keys.publicKey(header.getKeyId());
                        }
                    })
                    .clockSkewSeconds(60)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Rejected Google ID token: {}", ex.getMessage());
            throw new UnauthorizedException("Invalid Google ID token");
        }
        if (!ISSUERS.contains(claims.getIssuer())) {
            log.warn("Rejected Google ID token from issuer {}", claims.getIssuer());
            throw new UnauthorizedException("Invalid Google ID token");
        }
        Set<String> audience = claims.getAudience();
        if (audience == null || audience.stream().noneMatch(audiences::contains)) {
            log.warn("Rejected Google ID token for audience {}", audience);
            throw new UnauthorizedException("Google ID token was issued for another app");
        }
        Object verified = claims.get("email_verified");
        return new GoogleIdentity(
                claims.getSubject(),
                claims.get("email", String.class),
                Boolean.TRUE.equals(verified) || "true".equals(verified),
                claims.get("hd", String.class),
                claims.get("given_name", String.class),
                claims.get("family_name", String.class));
    }

    /** Test seam: replaces Google's key set without a network call. */
    void useKeys(Map<String, PublicKey> keys) {
        this.keys.useKeys(keys);
    }
}
