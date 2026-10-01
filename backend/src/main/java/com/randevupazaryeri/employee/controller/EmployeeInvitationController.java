package com.randevupazaryeri.employee.controller;

import com.randevupazaryeri.common.dto.ApiResponse;
import com.randevupazaryeri.employee.dto.CreateInvitationRequest;
import com.randevupazaryeri.employee.dto.EmployeeResponse;
import com.randevupazaryeri.employee.dto.InvitationResponse;
import com.randevupazaryeri.employee.service.EmployeeInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/employees/{employeeId}")
@RequiredArgsConstructor
@Tag(name = "Team")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeInvitationController {
    private final EmployeeInvitationService invitationService;

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Invite someone to take over this employee profile (owner)")
    public ApiResponse<InvitationResponse> invite(@PathVariable UUID businessId, @PathVariable UUID employeeId,
                                                  @Valid @RequestBody(required = false) CreateInvitationRequest request) {
        return ApiResponse.ok(invitationService.create(businessId, employeeId, request));
    }

    @GetMapping("/invitations")
    @Operation(summary = "Pending invitations for this employee (owner)")
    public ApiResponse<List<InvitationResponse>> pending(@PathVariable UUID businessId, @PathVariable UUID employeeId) {
        return ApiResponse.ok(invitationService.pendingForEmployee(businessId, employeeId));
    }

    @DeleteMapping("/invitations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke pending invitations for this employee (owner)")
    public void revoke(@PathVariable UUID businessId, @PathVariable UUID employeeId) {
        invitationService.revoke(businessId, employeeId);
    }

    @PostMapping("/account/self")
    @Operation(summary = "Link the owner's own account to this employee profile (owner works as an expert)")
    public ApiResponse<EmployeeResponse> linkOwner(@PathVariable UUID businessId, @PathVariable UUID employeeId) {
        return ApiResponse.ok(invitationService.linkOwner(businessId, employeeId));
    }

    @DeleteMapping("/account")
    @Operation(summary = "Remove the linked staff account from this employee (owner)")
    public ApiResponse<EmployeeResponse> unlink(@PathVariable UUID businessId, @PathVariable UUID employeeId) {
        return ApiResponse.ok(invitationService.unlinkAccount(businessId, employeeId));
    }
}
