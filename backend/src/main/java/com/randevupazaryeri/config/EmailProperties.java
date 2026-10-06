package com.randevupazaryeri.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.mail")
public class EmailProperties {
    /** Sender address, e.g. "resplz <no-reply@resplz.com>". Emails are skipped while empty. */
    private String from;

    /**
     * Resend API key. When set, email goes out over Resend's HTTPS API instead of SMTP, which hosts such as
     * Railway's Hobby plan block.
     */
    private String resendApiKey;

    public boolean usesResend() {
        return resendApiKey != null && !resendApiKey.isBlank();
    }
}
