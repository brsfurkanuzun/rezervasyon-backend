package com.randevupazaryeri.image.storage;

/**
 * Image storage provider abstraction. Business code depends only on this interface;
 * provider specifics (Cloudinary today, S3/R2 later) live in the implementations.
 */
public interface ImageStorageService {

    /** Stores an optimized rendition of the image. Throws {@link StorageException} on failure. */
    StoredImage upload(ImageUpload upload);

    /**
     * Removes the object. Idempotent: deleting an object that no longer exists succeeds.
     * Throws {@link StorageException} if the provider could not confirm the deletion.
     */
    void delete(StorageReference reference);

    /** Public delivery URL of the stored image, or {@code null} when the provider is unavailable. */
    String getUrl(StorageReference reference);

    /**
     * Delivery URL resized to at most {@code width} pixels, in the best format for the client,
     * or {@code null} when the provider is unavailable.
     */
    String getOptimizedUrl(StorageReference reference, int width);
}
