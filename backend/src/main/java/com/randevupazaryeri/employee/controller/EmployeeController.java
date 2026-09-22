package com.randevupazaryeri.employee.controller;

import com.randevupazaryeri.common.dto.ApiResponse;
import com.randevupazaryeri.employee.dto.*;
import com.randevupazaryeri.employee.service.EmployeeService;
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
@RequestMapping("/api/v1/businesses/{businessId}/employees")
@RequiredArgsConstructor
@Tag(name = "Employees")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
public class EmployeeController {
    private final EmployeeService employeeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create employee")
    public ApiResponse<EmployeeResponse> create(@PathVariable UUID businessId, @Valid @RequestBody CreateEmployeeRequest request) {
        return ApiResponse.ok(employeeService.create(businessId, request));
    }

    @GetMapping
    @Operation(summary = "List employees")
    public ApiResponse<List<EmployeeResponse>> list(@PathVariable UUID businessId) {
        return ApiResponse.ok(employeeService.list(businessId));
    }

    @PutMapping("/{employeeId}")
    @Operation(summary = "Update employee")
    public ApiResponse<EmployeeResponse> update(@PathVariable UUID businessId, @PathVariable UUID employeeId,
                                                @Valid @RequestBody CreateEmployeeRequest request) {
        return ApiResponse.ok(employeeService.update(businessId, employeeId, request));
    }

    @PutMapping("/{employeeId}/working-hours")
    @Operation(summary = "Replace working hours")
    public ApiResponse<List<WorkingHourResponse>> workingHours(@PathVariable UUID businessId, @PathVariable UUID employeeId,
                                                               @Valid @RequestBody List<WorkingHourRequest> request) {
        return ApiResponse.ok(employeeService.replaceWorkingHours(businessId, employeeId, request));
    }

    @GetMapping("/{employeeId}/working-hours")
    @Operation(summary = "List working hours")
    public ApiResponse<List<WorkingHourResponse>> listHours(@PathVariable UUID businessId, @PathVariable UUID employeeId) {
        return ApiResponse.ok(employeeService.listWorkingHours(businessId, employeeId));
    }

    @PostMapping("/{employeeId}/time-offs")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add time off")
    public ApiResponse<TimeOffResponse> addTimeOff(@PathVariable UUID businessId, @PathVariable UUID employeeId,
                                                   @Valid @RequestBody TimeOffRequest request) {
        return ApiResponse.ok(employeeService.addTimeOff(businessId, employeeId, request));
    }

    @DeleteMapping("/{employeeId}/time-offs/{timeOffId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete time off")
    public void deleteTimeOff(@PathVariable UUID businessId, @PathVariable UUID employeeId, @PathVariable UUID timeOffId) {
        employeeService.deleteTimeOff(businessId, employeeId, timeOffId);
    }
}
