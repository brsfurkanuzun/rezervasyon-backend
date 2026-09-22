package com.randevupazaryeri.availability.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data @Builder
public class AvailabilityResponse {
    private LocalDate date;
    private List<EmployeeAvailabilityResponse> employees;
}
