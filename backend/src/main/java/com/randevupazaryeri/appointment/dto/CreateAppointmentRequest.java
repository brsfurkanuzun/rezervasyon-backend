package com.randevupazaryeri.appointment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
public class CreateAppointmentRequest {
    @NotNull private UUID businessId;
    @NotNull private UUID employeeId;
    @NotNull private UUID serviceId;
    @NotNull private Instant startDateTime;
    private String customerNote;
}
