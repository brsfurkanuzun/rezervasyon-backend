package com.randevupazaryeri.image.dto;

import com.randevupazaryeri.image.entity.ImageFolder;
import com.randevupazaryeri.image.entity.ImageOwnerType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class ImageResponse {
    private UUID id;
    /** Optimized delivery URL; use as-is (do not build provider URLs on the client). */
    private String url;
    private String thumbnailUrl;
    private String publicId;
    private ImageFolder folder;
    private ImageOwnerType ownerType;
    private UUID ownerId;
    private Integer width;
    private Integer height;
    private String format;
    private Long bytes;
    private Instant createdAt;
}
