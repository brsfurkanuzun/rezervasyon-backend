package com.randevupazaryeri.image.storage;

public record StoredImage(
        StorageReference reference,
        String url,
        String resourceType,
        Integer width,
        Integer height,
        String format,
        Long bytes) {
}
