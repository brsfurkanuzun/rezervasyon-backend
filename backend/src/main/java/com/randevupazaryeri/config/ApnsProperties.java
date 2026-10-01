package com.randevupazaryeri.config;

import com.randevupazaryeri.push.entity.PushApp;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * APNs token auth settings. A team-scoped key can serve both apps via {@code key-id}/{@code private-key};
 * topic-specific keys go under {@code partner.*} / {@code customer.*} and take precedence.
 */
@Data
@ConfigurationProperties(prefix = "app.apns")
public class ApnsProperties {
    private String teamId;
    private String keyId;
    /** Contents of the AuthKey_XXXX.p8 file (PEM). Literal "\n" sequences are accepted. */
    private String privateKey;
    private Key partner = new Key();
    private Key customer = new Key();
    private String customerTopic = "com.rezplz.rezplz";
    private String partnerTopic = "com.rezplz.partner";

    @Data
    public static class Key {
        private String keyId;
        private String privateKey;

        boolean isComplete() {
            return hasText(keyId) && hasText(privateKey);
        }
    }

    /** The signing key for {@code app}, or {@code null} if push is not configured for it. */
    public Key keyFor(PushApp app) {
        if (!hasText(teamId)) {
            return null;
        }
        Key specific = app == PushApp.PARTNER ? partner : customer;
        if (specific != null && specific.isComplete()) {
            return specific;
        }
        Key shared = new Key();
        shared.setKeyId(keyId);
        shared.setPrivateKey(privateKey);
        return shared.isComplete() ? shared : null;
    }

    public String topicFor(PushApp app) {
        return app == PushApp.PARTNER ? partnerTopic : customerTopic;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
