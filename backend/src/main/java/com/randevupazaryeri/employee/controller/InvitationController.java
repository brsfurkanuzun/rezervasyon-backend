package com.randevupazaryeri.employee.controller;

import com.randevupazaryeri.common.dto.ApiResponse;
import com.randevupazaryeri.employee.dto.InvitationResponse;
import com.randevupazaryeri.employee.service.EmployeeInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
@Tag(name = "Team")
@SecurityRequirement(name = "bearerAuth")
public class InvitationController {
    private final EmployeeInvitationService invitationService;

    @GetMapping("/mine")
    @Operation(summary = "Pending team invitations sent to my email")
    public ApiResponse<List<InvitationResponse>> mine() {
        return ApiResponse.ok(invitationService.mine());
    }

    @GetMapping("/{code}")
    @Operation(summary = "Preview an invitation by code")
    public ApiResponse<InvitationResponse> preview(@PathVariable String code) {
        return ApiResponse.ok(invitationService.preview(code));
    }

    @PostMapping("/{code}/accept")
    @Operation(summary = "Join the team and link my account to the invited employee profile")
    public ApiResponse<InvitationResponse> accept(@PathVariable String code) {
        return ApiResponse.ok(invitationService.accept(code));
    }
}
