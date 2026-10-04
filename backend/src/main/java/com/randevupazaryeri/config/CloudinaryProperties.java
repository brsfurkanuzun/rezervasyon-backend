package com.randevupazaryeri.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Cloudinary credentials, supplied only through environment variables
 * ({@code CLOUDINARY_CLOUD_NAME}, {@code CLOUDINARY_API_KEY}, {@code CLOUDINARY_API_SECRET}).
 * {@link #toString()} deliberately omits the key and secret so the object is safe to log.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "cloudinary")
public class CloudinaryProperties {
    private String cloudName;
    private String apiKey;
    private String apiSecret;

    public boolean isConfigured() {
        return StringUtils.hasText(cloudName) && StringUtils.hasText(apiKey) && StringUtils.hasText(apiSecret);
    }

    @Override
    public String toString() {
        return "CloudinaryProperties{cloudName=" + cloudName + ", configured=" + isConfigured() + "}";
    }
}
