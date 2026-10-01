package com.randevupazaryeri.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.apple")
public class AppleProperties {
    /** Bundle ids allowed as the "aud" claim of Sign in with Apple identity tokens. */
    private List<String> audiences = List.of("com.rezplz.rezplz", "com.rezplz.partner");
}
