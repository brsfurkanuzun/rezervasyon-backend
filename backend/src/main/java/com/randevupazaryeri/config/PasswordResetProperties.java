package com.randevupazaryeri.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Pages that open the emailed reset link; the token goes in the URL fragment so it never reaches a server log. */
@Data
@ConfigurationProperties(prefix = "app.password-reset")
public class PasswordResetProperties {
    private String customerUrl;
    private String partnerUrl;
}
