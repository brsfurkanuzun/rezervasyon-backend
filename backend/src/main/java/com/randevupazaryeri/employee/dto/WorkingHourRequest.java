package com.randevupazaryeri.employee.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalTime;

@Data
public class WorkingHourRequest {
    @Min(1) @Max(7)
    private int dayOfWeek;
    @NotNull private LocalTime startTime;
    @NotNull private LocalTime endTime;
    private Boolean isAvailable = true;
}
