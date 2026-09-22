package com.randevupazaryeri.review.mapper;

import com.randevupazaryeri.review.dto.ReviewResponse;
import com.randevupazaryeri.review.entity.Review;

public final class ReviewMapper {
    private ReviewMapper() {}
    public static ReviewResponse toResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .customerId(r.getCustomer().getId())
                .customerName(r.getCustomer().getFirstName() + " " + r.getCustomer().getLastName())
                .businessId(r.getBusiness().getId())
                .appointmentId(r.getAppointment().getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
