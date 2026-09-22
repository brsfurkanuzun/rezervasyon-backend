package com.randevupazaryeri.serviceoffer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateServiceRequest {
    @NotBlank @Size(max = 200)
    private String name;
    private String description;
    @Min(5) @Max(24 * 60)
    private int durationMinutes;
    @NotNull @DecimalMin("0.0")
    private BigDecimal price;
    @Size(min = 3, max = 3)
    private String currency = "TRY";
}
