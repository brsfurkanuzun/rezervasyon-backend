package com.randevupazaryeri.review.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class ReviewResponse {
    private UUID id;
    private UUID customerId;
    private String customerName;
    private UUID businessId;
    private UUID appointmentId;
    private UUID employeeId;
    private String employeeName;
    private int rating;
    private String comment;
    private Instant createdAt;
}
