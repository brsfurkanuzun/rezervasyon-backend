package com.randevupazaryeri.business.dto;

import com.randevupazaryeri.business.entity.BusinessStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class BusinessSummaryResponse {
    private UUID id;
    private String name;
    private String slug;
    private String city;
    private String district;
    private String logoUrl;
    private String coverImageUrl;
    private BusinessStatus status;
    private Double averageRating;
    private long reviewCount;
    private BigDecimal startingPrice;
    private List<CategoryResponse> categories;
}
