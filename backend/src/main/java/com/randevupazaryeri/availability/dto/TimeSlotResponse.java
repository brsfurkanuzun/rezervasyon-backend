package com.randevupazaryeri.availability.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class TimeSlotResponse {
    private String start;
    private String end;
    private boolean available;
}
