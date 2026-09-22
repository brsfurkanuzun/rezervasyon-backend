package com.randevupazaryeri.review.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.UUID;

@Data
public class CreateReviewRequest {
    @NotNull private UUID appointmentId;
    @Min(1) @Max(5) private int rating;
    @Size(max = 2000) private String comment;
}
