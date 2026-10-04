package com.randevupazaryeri.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {
    /** Keep in sync with {@code spring.servlet.multipart.max-file-size}. */
    private DataSize maxFileSize = DataSize.ofMegabytes(5);
    /** Top-level storage folder; every object key is built server-side beneath it. */
    private String rootFolder = "resplz";
    private boolean allowAvif = true;
    /** Rejects decompression bombs: width × height must not exceed this. */
    private long maxPixels = 40_000_000L;
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class RateLimit {
        private int maxUploads = 30;
        private Duration window = Duration.ofMinutes(10);
    }
}
