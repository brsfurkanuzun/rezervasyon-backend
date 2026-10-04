package com.randevupazaryeri.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.google")
public class GoogleProperties {
    /** OAuth client id used by the websites. Public by design; Google sign-in is disabled while empty. */
    private String webClientId;

    /** Further OAuth client ids (e.g. the iOS apps) accepted as the "aud" claim of Google ID tokens. */
    private List<String> extraClientIds = new ArrayList<>();

    public List<String> audiences() {
        List<String> audiences = new ArrayList<>();
        if (webClientId != null && !webClientId.isBlank()) {
            audiences.add(webClientId.trim());
        }
        extraClientIds.stream().filter(id -> id != null && !id.isBlank()).map(String::trim).forEach(audiences::add);
        return audiences;
    }
}
