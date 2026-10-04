package com.randevupazaryeri.config;

import com.cloudinary.Cloudinary;
import com.randevupazaryeri.image.storage.CloudinaryImageStorageService;
import com.randevupazaryeri.image.storage.ImageStorageService;
import com.randevupazaryeri.image.storage.UnavailableImageStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryConfigTest {

    @Configuration
    @EnableConfigurationProperties(CloudinaryProperties.class)
    static class Props {
    }

    final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Props.class, CloudinaryConfig.class);

    @Test
    void usesCloudinaryWhenAllCredentialsArePresent() {
        runner.withPropertyValues("cloudinary.cloud-name=test-cloud", "cloudinary.api-key=fake-key",
                        "cloudinary.api-secret=fake-secret")
                .run(context -> {
                    assertThat(context).hasSingleBean(Cloudinary.class);
                    assertThat(context.getBean(ImageStorageService.class)).isInstanceOf(CloudinaryImageStorageService.class);
                    assertThat(context.getBean(CloudinaryProperties.class).toString()).doesNotContain("fake-secret")
                            .doesNotContain("fake-key");
                });
    }

    @Test
    void startsWithoutCloudinaryWhenCredentialsAreMissingOrBlank() {
        runner.withPropertyValues("cloudinary.cloud-name=test-cloud", "cloudinary.api-key=", "cloudinary.api-secret=")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(Cloudinary.class);
                    assertThat(context.getBean(ImageStorageService.class)).isInstanceOf(UnavailableImageStorageService.class);
                });
    }
}
