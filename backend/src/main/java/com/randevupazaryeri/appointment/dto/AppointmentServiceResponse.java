package com.randevupazaryeri.appointment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class AppointmentServiceResponse {
    private UUID serviceId;
    private String serviceName;
    private int durationMinutes;
    private BigDecimal price;
    private String currency;
}