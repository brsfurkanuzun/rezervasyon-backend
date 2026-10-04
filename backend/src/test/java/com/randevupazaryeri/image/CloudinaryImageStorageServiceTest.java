package com.randevupazaryeri.image;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.Uploader;
import com.randevupazaryeri.config.CloudinaryProperties;
import com.randevupazaryeri.image.storage.CloudinaryImageStorageService;
import com.randevupazaryeri.image.storage.ImageUpload;
import com.randevupazaryeri.image.storage.StorageException;
import com.randevupazaryeri.image.storage.StorageReference;
import com.randevupazaryeri.image.storage.StoredImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Uses a mocked Cloudinary SDK and obviously fake configuration values; no network calls. */
@ExtendWith(OutputCaptureExtension.class)
class CloudinaryImageStorageServiceTest {

    static final String FAKE_SECRET = "fake-test-secret-value";
    static final String FAKE_KEY = "000000000000000";

    Cloudinary cloudinary;
    Uploader uploader;
    CloudinaryImageStorageService service;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        service = new CloudinaryImageStorageService(cloudinary, properties());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void uploadUsesServerBuiltPublicIdAndOptimizesToCappedWebp() throws IOException {
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "public_id", "resplz/businesses/b1/cover/n1",
                "secure_url", "https://res.cloudinary.com/test-cloud/image/upload/v1/resplz/businesses/b1/cover/n1.webp",
                "resource_type", "image", "width", 1600, "height", 900, "format", "webp", "bytes", 12345));

        StoredImage stored = service.upload(new ImageUpload(new byte[]{1, 2, 3}, "image/png",
                "resplz/businesses/b1/cover", "n1", 1600));

        ArgumentCaptor<Map> params = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(uploader).upload(any(), params.capture());
        Map<String, Object> sent = params.getValue();
        assertThat(sent).containsEntry("public_id", "resplz/businesses/b1/cover/n1")
                .containsEntry("format", "webp")
                .containsEntry("overwrite", false)
                .containsEntry("resource_type", "image");
        assertThat(((Transformation) sent.get("transformation")).generate())
                .contains("w_1600").contains("c_limit").contains("q_auto");

        assertThat(stored.reference()).isEqualTo(new StorageReference("cloudinary", "resplz/businesses/b1/cover/n1"));
        assertThat(stored.width()).isEqualTo(1600);
        assertThat(stored.bytes()).isEqualTo(12345L);
        assertThat(stored.format()).isEqualTo("webp");
    }

    @Test
    void uploadFailureIsWrappedWithoutLeakingCredentials(CapturedOutput output) throws IOException {
        when(uploader.upload(any(), anyMap()))
                .thenThrow(new IOException("Invalid Signature for api_key=" + FAKE_KEY + " secret=" + FAKE_SECRET));

        assertThatThrownBy(() -> service.upload(new ImageUpload(new byte[]{1}, "image/png", "resplz/users/u1/avatar", "n", 800)))
                .isInstanceOf(StorageException.class)
                .hasMessageNotContaining(FAKE_SECRET)
                .hasMessageNotContaining(FAKE_KEY);
        assertThat(output.getAll()).doesNotContain(FAKE_SECRET).doesNotContain(FAKE_KEY).contains("[redacted]");
    }

    @Test
    void deleteIsIdempotentForMissingAssets() throws IOException {
        when(uploader.destroy(eq("resplz/users/u1/avatar/n"), anyMap())).thenReturn(Map.of("result", "not found"));

        assertThatCode(() -> service.delete(new StorageReference("cloudinary", "resplz/users/u1/avatar/n")))
                .doesNotThrowAnyException();
    }

    @Test
    void deleteFailureRaisesStorageException() throws IOException {
        when(uploader.destroy(any(), anyMap())).thenReturn(Map.of("result", "error"));
        assertThatThrownBy(() -> service.delete(new StorageReference("cloudinary", "k")))
                .isInstanceOf(StorageException.class);

        when(uploader.destroy(any(), anyMap())).thenThrow(new IOException("timeout"));
        assertThatThrownBy(() -> service.delete(new StorageReference("cloudinary", "k")))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void rejectsReferencesFromOtherProviders() {
        assertThatThrownBy(() -> service.delete(new StorageReference("s3", "k")))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void buildsOptimizedDeliveryUrls() {
        CloudinaryImageStorageService real = new CloudinaryImageStorageService(new Cloudinary(Map.of(
                "cloud_name", "test-cloud", "api_key", FAKE_KEY, "api_secret", FAKE_SECRET, "secure", true)), properties());
        StorageReference ref = new StorageReference("cloudinary", "resplz/businesses/b1/gallery/n1");

        assertThat(real.getUrl(ref))
                .startsWith("https://res.cloudinary.com/test-cloud/image/upload/")
                .contains("/resplz/businesses/b1/gallery/n1");
        assertThat(real.getOptimizedUrl(ref, 400))
                .startsWith("https://res.cloudinary.com/test-cloud/image/upload/")
                .contains("w_400").contains("c_limit").contains("f_auto").contains("q_auto")
                .doesNotContain(FAKE_SECRET);
    }

    private static CloudinaryProperties properties() {
        CloudinaryProperties properties = new CloudinaryProperties();
        properties.setCloudName("test-cloud");
        properties.setApiKey(FAKE_KEY);
        properties.setApiSecret(FAKE_SECRET);
        return properties;
    }
}
