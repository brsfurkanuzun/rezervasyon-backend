package com.randevupazaryeri.favorite.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class FavoriteResponse {
    private UUID id;
    private UUID businessId;
    private String businessName;
    private String businessSlug;
    private Instant createdAt;
}
