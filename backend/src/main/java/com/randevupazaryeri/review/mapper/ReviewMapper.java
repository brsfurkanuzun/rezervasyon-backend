package com.randevupazaryeri.review.mapper;

import com.randevupazaryeri.review.dto.ReviewResponse;
import com.randevupazaryeri.review.entity.Review;

public final class ReviewMapper {
    private ReviewMapper() {}
    public static ReviewResponse toResponse(Review r) {
        var employee = r.getAppointment().getEmployee();
        return ReviewResponse.builder()
                .id(r.getId())
                .customerId(r.getCustomer().getId())
                .customerName(r.getCustomer().getFirstName() + " " + r.getCustomer().getLastName())
                .businessId(r.getBusiness().getId())
                .appointmentId(r.getAppointment().getId())
                .employeeId(employee != null ? employee.getId() : null)
                .employeeName(employee != null
                        ? employee.getFirstName() + " " + employee.getLastName()
                        : null)
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
