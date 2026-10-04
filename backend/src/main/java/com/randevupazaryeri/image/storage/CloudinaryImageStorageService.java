package com.randevupazaryeri.image.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.randevupazaryeri.config.CloudinaryProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Cloudinary-backed storage. All Cloudinary-specific parameters and URL rules stay in this class.
 * Provider errors are logged without credentials and rethrown as {@link StorageException}.
 */
@Slf4j
public class CloudinaryImageStorageService implements ImageStorageService {

    public static final String PROVIDER = "cloudinary";

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;

    public CloudinaryImageStorageService(Cloudinary cloudinary, CloudinaryProperties properties) {
        this.cloudinary = cloudinary;
        this.properties = properties;
    }

    @Override
    public StoredImage upload(ImageUpload upload) {
        String publicId = upload.directory() + "/" + upload.name();
        Map<String, Object> params = new HashMap<>();
        params.put("public_id", publicId);
        params.put("asset_folder", upload.directory());
        params.put("resource_type", "image");
        params.put("overwrite", false);
        params.put("unique_filename", false);
        params.put("use_filename", false);
        // Stored once as a size-capped WebP; the original file is never kept or served.
        params.put("transformation", limitWidth(upload.maxWidth()).quality("auto"));
        params.put("format", "webp");

        Map<?, ?> result;
        try {
            result = cloudinary.uploader().upload(upload.content(), params);
        } catch (Exception e) {
            log.error("Cloudinary upload failed publicId={} bytes={} error={}: {}",
                    publicId, upload.content().length, e.getClass().getSimpleName(), redact(e.getMessage()));
            throw new StorageException("Image upload failed");
        }

        String storedId = string(result.get("public_id"));
        String url = string(result.get("secure_url"));
        if (!StringUtils.hasText(storedId) || !StringUtils.hasText(url)) {
            log.error("Cloudinary upload returned an incomplete response publicId={}", publicId);
            throw new StorageException("Image upload failed");
        }
        return new StoredImage(
                new StorageReference(PROVIDER, storedId),
                url,
                string(result.get("resource_type")),
                integer(result.get("width")),
                integer(result.get("height")),
                string(result.get("format")),
                longValue(result.get("bytes")));
    }

    @Override
    public void delete(StorageReference reference) {
        requireOwnReference(reference);
        Map<?, ?> result;
        try {
            result = cloudinary.uploader().destroy(reference.key(),
                    Map.of("resource_type", "image", "invalidate", true));
        } catch (Exception e) {
            log.error("Cloudinary delete failed publicId={} error={}: {}",
                    reference.key(), e.getClass().getSimpleName(), redact(e.getMessage()));
            throw new StorageException("Image delete failed");
        }
        String outcome = string(result.get("result"));
        if (!"ok".equals(outcome) && !"not found".equals(outcome)) {
            log.error("Cloudinary delete not confirmed publicId={} result={}", reference.key(), outcome);
            throw new StorageException("Image delete failed");
        }
    }

    @Override
    public String getUrl(StorageReference reference) {
        requireOwnReference(reference);
        return cloudinary.url().secure(true).resourceType("image").type("upload").generate(reference.key());
    }

    @Override
    public String getOptimizedUrl(StorageReference reference, int width) {
        requireOwnReference(reference);
        return cloudinary.url().secure(true).resourceType("image").type("upload")
                .transformation(limitWidth(width).quality("auto").fetchFormat("auto"))
                .generate(reference.key());
    }

    @SuppressWarnings("rawtypes")
    private static Transformation limitWidth(int width) {
        return new Transformation().width(width).crop("limit");
    }

    private static void requireOwnReference(StorageReference reference) {
        if (reference == null || !PROVIDER.equals(reference.provider()) || !StringUtils.hasText(reference.key())) {
            throw new StorageException("Reference does not belong to the Cloudinary provider");
        }
    }

    /** Provider messages can echo request parameters; never let credentials reach the logs. */
    private String redact(String message) {
        if (message == null) {
            return "";
        }
        String safe = message;
        for (String secret : new String[]{properties.getApiSecret(), properties.getApiKey()}) {
            if (StringUtils.hasText(secret)) {
                safe = safe.replace(secret, "[redacted]");
            }
        }
        return safe.length() > 300 ? safe.substring(0, 300) : safe;
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private static Integer integer(Object value) {
        return value instanceof Number n ? n.intValue() : null;
    }

    private static Long longValue(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }
}
