package com.randevupazaryeri.business.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;

@Data
@AllArgsConstructor
public class OpeningHourResponse {
    /** ISO-8601 day: 1=Monday ... 7=Sunday */
    private int dayOfWeek;
    private LocalTime openTime;
    private LocalTime closeTime;
}
