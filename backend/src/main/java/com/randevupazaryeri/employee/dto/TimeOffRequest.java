package com.randevupazaryeri.employee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.Instant;

@Data
public class TimeOffRequest {
    private String title;
    @NotNull private Instant startAt;
    @NotNull private Instant endAt;
}
