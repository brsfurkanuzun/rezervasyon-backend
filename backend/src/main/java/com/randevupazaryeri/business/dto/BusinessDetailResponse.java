package com.randevupazaryeri.business.dto;

import com.randevupazaryeri.business.entity.BusinessStatus;
import com.randevupazaryeri.employee.dto.EmployeeResponse;
import com.randevupazaryeri.review.dto.ReviewResponse;
import com.randevupazaryeri.serviceoffer.dto.ServiceResponse;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class BusinessDetailResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String phone;
    private String email;
    private String address;
    private String city;
    private String district;
    private Double latitude;
    private Double longitude;
    private String logoUrl;
    private String coverImageUrl;
    private String timezone;
    private boolean autoConfirm;
    private BusinessStatus status;
    private Double averageRating;
    private long reviewCount;
    private List<CategoryResponse> categories;
    private List<ServiceResponse> services;
    private List<EmployeeResponse> employees;
    private List<ReviewResponse> recentReviews;
}
