package com.randevupazaryeri.image.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadata of a stored image. The binary lives in the storage provider, never in PostgreSQL.
 * Deliberately holds no personal data beyond the uploader's user id.
 */
@Entity
@Table(name = "images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Image {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "storage_provider", nullable = false, length = 32)
    private String storageProvider;

    /** Provider object key (Cloudinary {@code public_id}). */
    @Column(name = "public_id", nullable = false, length = 300)
    private String publicId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "resource_type", nullable = false, length = 32)
    private String resourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ImageFolder folder;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 32)
    private ImageOwnerType ownerType;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    private Integer width;
    private Integer height;

    @Column(length = 16)
    private String format;

    private Long bytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
