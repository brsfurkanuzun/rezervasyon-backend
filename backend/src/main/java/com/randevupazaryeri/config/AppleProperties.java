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

    public List<String> allowedAudiences() {
        List<String> allowed = new ArrayList<>(audiences);
        if (webServiceId != null && !webServiceId.isBlank()) {
            allowed.add(webServiceId.trim());
        }
        return allowed;
    }
}
