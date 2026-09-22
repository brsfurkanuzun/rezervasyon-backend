package com.randevupazaryeri.serviceoffer.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data @Builder
public class ServiceResponse {
    private UUID id;
    private String name;
    private String description;
    private int durationMinutes;
    private BigDecimal price;
    private String currency;
    private boolean isActive;
}
