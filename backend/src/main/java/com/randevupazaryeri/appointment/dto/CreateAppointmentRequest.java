package com.randevupazaryeri.appointment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class CreateAppointmentRequest {
    @NotNull private UUID businessId;
    @NotNull private UUID employeeId;
    private UUID serviceId;
    private List<UUID> serviceIds;
    @NotNull private Instant startDateTime;
    private String customerNote;
}
