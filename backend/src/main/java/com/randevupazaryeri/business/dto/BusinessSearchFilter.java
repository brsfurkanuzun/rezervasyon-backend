package com.randevupazaryeri.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Query parameters of {@code GET /api/v1/businesses}. The bounding box applies only when all four
 * edges are present; {@code minLng > maxLng} means the box crosses the antimeridian.
 */
public record BusinessSearchFilter(
        String query,
        String category,
        String city,
        String district,
        @Schema(description = "Lowest starting price (cheapest active service)") BigDecimal minPrice,
        @Schema(description = "Highest starting price; businesses without services are kept") BigDecimal maxPrice,
        @Schema(description = "Minimum average rating") Double rating,
        @Schema(description = "South edge of the map viewport") Double minLat,
        @Schema(description = "North edge of the map viewport") Double maxLat,
        @Schema(description = "West edge of the map viewport") Double minLng,
        @Schema(description = "East edge of the map viewport") Double maxLng,
        @Schema(description = "Caller latitude, used for distanceKm and sortBy=distance") Double lat,
        @Schema(description = "Caller longitude, used for distanceKm and sortBy=distance") Double lng,
        @Schema(description = "distance | rating; otherwise the pageable sort applies", allowableValues = {"distance", "rating"}) String sortBy
) {

    public boolean hasBounds() {
        return minLat != null && maxLat != null && minLng != null && maxLng != null;
    }

    public boolean hasOrigin() {
        return lat != null && lng != null;
    }
}
