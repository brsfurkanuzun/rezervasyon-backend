package com.randevupazaryeri.appointment.controller;

import com.randevupazaryeri.appointment.dto.*;
import com.randevupazaryeri.appointment.service.AppointmentService;
import com.randevupazaryeri.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @Operation(summary = "Create appointment")
    public ApiResponse<AppointmentResponse> create(@Valid @RequestBody CreateAppointmentRequest request) {
        return ApiResponse.ok(appointmentService.create(request));
    }

    @GetMapping("/appointments/my")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @Operation(summary = "My appointments")
    public ApiResponse<List<AppointmentResponse>> my(@PageableDefault(size = 20) Pageable pageable) {
        Page<AppointmentResponse> page = appointmentService.myAppointments(pageable);
        return ApiResponse.ofPage(page);
    }

    @PostMapping("/appointments/{id}/cancel")
    @Operation(summary = "Cancel appointment")
    public ApiResponse<AppointmentResponse> cancel(@PathVariable UUID id, @RequestBody(required = false) CancelAppointmentRequest request) {
        return ApiResponse.ok(appointmentService.cancel(id, request != null ? request : new CancelAppointmentRequest()));
    }

    @GetMapping("/businesses/{businessId}/appointments")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @Operation(summary = "Business appointments")
    public ApiResponse<List<AppointmentResponse>> business(@PathVariable UUID businessId, @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ofPage(appointmentService.businessAppointments(businessId, pageable));
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
