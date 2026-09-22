package com.randevupazaryeri.business.mapper;

import com.randevupazaryeri.business.dto.BusinessSummaryResponse;
import com.randevupazaryeri.business.dto.CategoryResponse;
import com.randevupazaryeri.business.entity.Business;
import com.randevupazaryeri.business.entity.Category;

import java.math.BigDecimal;
import java.util.List;

public final class BusinessMapper {
    private BusinessMapper() {}

    public static CategoryResponse toCategory(Category c) {
        return CategoryResponse.builder()
                .id(c.getId()).code(c.getCode()).name(c.getName()).description(c.getDescription()).build();
    }

    public static BusinessSummaryResponse toSummary(Business b, Double avg, long count, BigDecimal startingPrice) {
        return BusinessSummaryResponse.builder()
                .id(b.getId()).name(b.getName()).slug(b.getSlug())
                .city(b.getCity()).district(b.getDistrict())
                .logoUrl(b.getLogoUrl()).coverImageUrl(b.getCoverImageUrl())
                .status(b.getStatus())
                .averageRating(avg).reviewCount(count).startingPrice(startingPrice)
                .categories(b.getCategories().stream().map(BusinessMapper::toCategory).toList())
                .build();
    }
}
