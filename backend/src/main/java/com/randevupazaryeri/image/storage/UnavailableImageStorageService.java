package com.randevupazaryeri.image.storage;

/**
 * Used when no storage provider is configured: the app runs, uploads and deletes fail cleanly.
 * URL lookups return {@code null} so callers fall back to the URL saved at upload time.
 */
public class UnavailableImageStorageService implements ImageStorageService {

    @Override
    public StoredImage upload(ImageUpload upload) {
        throw new StorageException("Image storage is not configured");
    }

    @Override
    public void delete(StorageReference reference) {
        throw new StorageException("Image storage is not configured");
    }

    @Override
    public String getUrl(StorageReference reference) {
        return null;
    }

    @Override
    public String getOptimizedUrl(StorageReference reference, int width) {
        return null;
    }
}
