package com.randevupazaryeri.business.controller;

import com.randevupazaryeri.business.dto.WorkplaceResponse;
import com.randevupazaryeri.business.service.BusinessService;
import com.randevupazaryeri.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/provider/workplaces")
@RequiredArgsConstructor
@Tag(name = "Provider Businesses")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
public class ProviderWorkplaceController {
    private final BusinessService businessService;

    @GetMapping
    @Operation(summary = "Businesses the current provider owns or works at as staff")
    public ApiResponse<List<WorkplaceResponse>> myWorkplaces() {
        return ApiResponse.ok(businessService.myWorkplaces());
    }
}
