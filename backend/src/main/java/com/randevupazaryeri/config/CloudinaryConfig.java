package com.randevupazaryeri.config;

import com.cloudinary.Cloudinary;
import com.randevupazaryeri.image.storage.CloudinaryImageStorageService;
import com.randevupazaryeri.image.storage.ImageStorageService;
import com.randevupazaryeri.image.storage.UnavailableImageStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Wires the image storage provider. Without Cloudinary credentials the application still starts
 * (so existing features keep working) and uploads fail with a generic storage error.
 */
@Slf4j
@Configuration
public class CloudinaryConfig {

    @Bean
    @Conditional(CloudinaryCredentialsPresent.class)
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        return new Cloudinary(Map.of(
                "cloud_name", properties.getCloudName(),
                "api_key", properties.getApiKey(),
                "api_secret", properties.getApiSecret(),
                "secure", true));
    }

    @Bean
    public ImageStorageService imageStorageService(ObjectProvider<Cloudinary> cloudinary,
                                                   CloudinaryProperties properties) {
        Cloudinary client = cloudinary.getIfAvailable();
        if (client == null) {
            log.warn("Cloudinary is not configured (CLOUDINARY_* variables missing); image uploads are disabled");
            return new UnavailableImageStorageService();
        }
        log.info("Image storage provider: cloudinary (cloud={})", properties.getCloudName());
        return new CloudinaryImageStorageService(client, properties);
    }

    static class CloudinaryCredentialsPresent implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            var env = context.getEnvironment();
            return StringUtils.hasText(env.getProperty("cloudinary.cloud-name"))
                    && StringUtils.hasText(env.getProperty("cloudinary.api-key"))
                    && StringUtils.hasText(env.getProperty("cloudinary.api-secret"));
        }
    }
}
