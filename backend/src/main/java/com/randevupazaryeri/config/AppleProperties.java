package com.randevupazaryeri.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.apple")
public class AppleProperties {
    /** Bundle ids allowed as the "aud" claim of Sign in with Apple identity tokens. */
    private List<String> audiences = List.of("com.rezplz.rezplz", "com.rezplz.partner");

    /** Services ID used by the websites. Public by design; web Apple sign-in is disabled while empty. */
    private String webServiceId;

    /** Apple Developer team id; signs the client secret used to revoke tokens. */
    private String teamId;

    /** Id of the .p8 key with "Sign in with Apple" enabled. Token revocation is skipped while empty. */
    private String signInKeyId;

    /** Contents of that .p8 key (PEM or base64, "\n" escapes allowed). Server-side only. */
    private String signInPrivateKey;

    public boolean canRevokeTokens() {
        return teamId != null && !teamId.isBlank()
                && signInKeyId != null && !signInKeyId.isBlank()
                && signInPrivateKey != null && !signInPrivateKey.isBlank();
    }

    public List<String> allowedAudiences() {
        List<String> allowed = new ArrayList<>(audiences);
        if (webServiceId != null && !webServiceId.isBlank()) {
            allowed.add(webServiceId.trim());
        }
        return allowed;
    }
}
