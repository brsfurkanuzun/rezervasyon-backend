package com.randevupazaryeri.business.controller;

import com.randevupazaryeri.business.dto.BusinessSummaryResponse;
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
@RequestMapping("/api/v1/provider/businesses")
@RequiredArgsConstructor
@Tag(name = "Provider Businesses")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
public class ProviderBusinessController {
    private final BusinessService businessService;

    @GetMapping
    @Operation(summary = "List businesses owned by current provider")
    public ApiResponse<List<BusinessSummaryResponse>> myBusinesses() {
        return ApiResponse.ok(businessService.myBusinesses());
    }
}
