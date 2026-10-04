package com.randevupazaryeri.image.storage;

/**
 * Provider-neutral pointer to a stored object. {@code key} is the provider's identifier
 * (Cloudinary {@code public_id}, S3 object key, ...).
 */
public record StorageReference(String provider, String key) {
}
