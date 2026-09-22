package com.randevupazaryeri.employee.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;
import java.util.UUID;

@Data @Builder
public class WorkingHourResponse {
    private UUID id;
    private int dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isAvailable;
}
