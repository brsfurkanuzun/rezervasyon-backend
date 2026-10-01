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
@RequestMapping("/api/v1/businesses/{businessId}")
@RequiredArgsConstructor
@Tag(name = "Team")
@SecurityRequirement(name = "bearerAuth")
public class TeamController {
    private final EmployeeInvitationService invitationService;

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "New single-use code for a new expert; their profile is created when they join (owner)")
    public ApiResponse<InvitationResponse> invite(@PathVariable UUID businessId,
                                                  @Valid @RequestBody(required = false) CreateInvitationRequest request) {
        return ApiResponse.ok(invitationService.createOpen(businessId, request));
    }

    @GetMapping("/invitations")
    @Operation(summary = "Pending codes for new experts (owner)")
    public ApiResponse<List<InvitationResponse>> pending(@PathVariable UUID businessId) {
        return ApiResponse.ok(invitationService.pendingOpen(businessId));
    }

    @DeleteMapping("/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke an invitation (owner)")
    public void revoke(@PathVariable UUID businessId, @PathVariable UUID invitationId) {
        invitationService.revokeById(businessId, invitationId);
    }

    @PostMapping("/employees/self")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add the owner to the team as an expert with a new profile (owner)")
    public ApiResponse<EmployeeResponse> addSelf(@PathVariable UUID businessId) {
        return ApiResponse.ok(invitationService.addOwnerAsEmployee(businessId));
    }
}
