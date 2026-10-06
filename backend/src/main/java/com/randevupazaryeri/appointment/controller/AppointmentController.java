package com.randevupazaryeri.appointment.controller;

import com.randevupazaryeri.appointment.dto.*;
import com.randevupazaryeri.appointment.service.AppointmentService;
import com.randevupazaryeri.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Appointments")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CUSTOMER','PROVIDER','ADMIN')")
    @Operation(summary = "Create appointment")
    public ApiResponse<AppointmentResponse> create(@Valid @RequestBody CreateAppointmentRequest request) {
        return ApiResponse.ok(appointmentService.create(request));
    }

    @GetMapping("/appointments/my")
    @PreAuthorize("hasAnyRole('CUSTOMER','PROVIDER','ADMIN')")
    @Operation(summary = "My appointments")
    public ApiResponse<List<AppointmentResponse>> my(@PageableDefault(size = 20) Pageable pageable) {
        Page<AppointmentResponse> page = appointmentService.myAppointments(pageable);
        return ApiResponse.ofPage(page);
    }

    @GetMapping("/appointments/{id}")
    @Operation(summary = "One appointment, for its customer, business owner or assigned expert")
    public ApiResponse<AppointmentResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(appointmentService.getForViewer(id));
    }

    @PostMapping("/appointments/{id}/cancel")
    @Operation(summary = "Cancel appointment")
    public ApiResponse<AppointmentResponse> cancel(@PathVariable UUID id, @RequestBody(required = false) CancelAppointmentRequest request) {
        return ApiResponse.ok(appointmentService.cancel(id, request != null ? request : new CancelAppointmentRequest()));
    }

    @GetMapping("/businesses/{businessId}/appointments")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @Operation(summary = "Business appointments")
    public ApiResponse<List<AppointmentResponse>> business(
            @PathVariable UUID businessId,
            @Parameter(description = "Only appointments starting at or after this instant (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "Only appointments starting before this instant (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ofPage(appointmentService.businessAppointments(businessId, from, to, pageable));
    }

    @PostMapping("/businesses/{businessId}/appointments/{id}/confirm")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ApiResponse<AppointmentResponse> confirm(@PathVariable UUID businessId, @PathVariable UUID id) {
        return ApiResponse.ok(appointmentService.confirm(businessId, id));
    }

    @PostMapping("/businesses/{businessId}/appointments/{id}/complete")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ApiResponse<AppointmentResponse> complete(@PathVariable UUID businessId, @PathVariable UUID id) {
        return ApiResponse.ok(appointmentService.complete(businessId, id));
    }

    @PostMapping("/businesses/{businessId}/appointments/{id}/no-show")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ApiResponse<AppointmentResponse> noShow(@PathVariable UUID businessId, @PathVariable UUID id) {
        return ApiResponse.ok(appointmentService.noShow(businessId, id));
    }
}
