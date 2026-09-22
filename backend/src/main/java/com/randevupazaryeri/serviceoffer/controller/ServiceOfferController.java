package com.randevupazaryeri.serviceoffer.controller;

import com.randevupazaryeri.common.dto.ApiResponse;
import com.randevupazaryeri.serviceoffer.dto.CreateServiceRequest;
import com.randevupazaryeri.serviceoffer.dto.ServiceResponse;
import com.randevupazaryeri.serviceoffer.service.ServiceOfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/services")
@RequiredArgsConstructor
@Tag(name = "Services")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
public class ServiceOfferController {
    private final ServiceOfferService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create service")
    public ApiResponse<ServiceResponse> create(@PathVariable UUID businessId, @Valid @RequestBody CreateServiceRequest request) {
        return ApiResponse.ok(service.create(businessId, request));
    }

    @GetMapping
    @Operation(summary = "List services")
    public ApiResponse<List<ServiceResponse>> list(@PathVariable UUID businessId) {
        return ApiResponse.ok(service.list(businessId));
    }

    @PutMapping("/{serviceId}")
    @Operation(summary = "Update service")
    public ApiResponse<ServiceResponse> update(@PathVariable UUID businessId, @PathVariable UUID serviceId,
                                               @Valid @RequestBody CreateServiceRequest request) {
        return ApiResponse.ok(service.update(businessId, serviceId, request));
    }

    @DeleteMapping("/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate service")
    public void deactivate(@PathVariable UUID businessId, @PathVariable UUID serviceId) {
        service.deactivate(businessId, serviceId);
    }
}
