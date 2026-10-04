package com.randevupazaryeri.image.entity;

import java.util.UUID;

/**
 * The only accepted upload destinations. Storage paths are derived from this enum and
 * server-side ids; client-supplied strings never become part of a path.
 */
public enum ImageFolder {
    BUSINESS_PROFILE(ImageOwnerType.BUSINESS, "businesses", "profile", 1200, true),
    BUSINESS_COVER(ImageOwnerType.BUSINESS, "businesses", "cover", 1600, true),
    BUSINESS_GALLERY(ImageOwnerType.BUSINESS, "businesses", "gallery", 1600, false),
    SERVICE_IMAGE(ImageOwnerType.SERVICE, "services", null, 1200, true),
    USER_AVATAR(ImageOwnerType.USER, "users", "avatar", 800, true);

    private final ImageOwnerType ownerType;
    private final String collection;
    private final String slot;
    private final int maxWidth;
    private final boolean single;

    ImageFolder(ImageOwnerType ownerType, String collection, String slot, int maxWidth, boolean single) {
        this.ownerType = ownerType;
        this.collection = collection;
        this.slot = slot;
        this.maxWidth = maxWidth;
        this.single = single;
    }

    public ImageOwnerType ownerType() {
        return ownerType;
    }

    public int maxWidth() {
        return maxWidth;
    }

    /** Single-slot folders hold one current image; uploading replaces the previous one. */
    public boolean single() {
        return single;
    }

    /** e.g. {@code resplz/businesses/{id}/cover}, {@code resplz/services/{id}}. */
    public String directory(String root, UUID ownerId) {
        String base = root + "/" + collection + "/" + ownerId;
        return slot == null ? base : base + "/" + slot;
    }
}
