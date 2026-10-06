package com.randevupazaryeri.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.mail")
public class EmailProperties {
    /** Sender address, e.g. "resplz <no-reply@resplz.com>". Emails are skipped while empty. */
    private String from;
}
