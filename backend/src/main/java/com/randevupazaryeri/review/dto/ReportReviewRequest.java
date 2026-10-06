package com.randevupazaryeri.review.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportReviewRequest {
    @Size(max = 500)
    private String reason;
}
