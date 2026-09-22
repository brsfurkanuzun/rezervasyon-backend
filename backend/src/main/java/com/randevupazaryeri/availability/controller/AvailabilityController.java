package com.randevupazaryeri.availability.controller;

import com.randevupazaryeri.availability.dto.AvailabilityResponse;
import com.randevupazaryeri.availability.service.AvailabilityService;
import com.randevupazaryeri.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/availability")
@RequiredArgsConstructor
@Tag(name = "Availability")
public class AvailabilityController {
    private final AvailabilityService availabilityService;

    @GetMapping
    @Operation(summary = "Compute available appointment slots")
    public ApiResponse<AvailabilityResponse> get(
            @PathVariable UUID businessId,
            @RequestParam UUID serviceId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(availabilityService.getAvailability(businessId, serviceId, employeeId, date));
    }
}
