package com.randevupazaryeri.availability.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class EmployeeAvailabilityResponse {
    private UUID employeeId;
    private String employeeName;
    private List<TimeSlotResponse> slots;
}
