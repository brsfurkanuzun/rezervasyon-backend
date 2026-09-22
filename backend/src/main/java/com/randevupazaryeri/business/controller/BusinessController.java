package com.randevupazaryeri.business.controller;

import com.randevupazaryeri.business.dto.*;
import com.randevupazaryeri.business.service.BusinessService;
import com.randevupazaryeri.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses")
@RequiredArgsConstructor
@Tag(name = "Businesses")
public class BusinessController {

    private final BusinessService businessService;

    @GetMapping
    @Operation(summary = "Search businesses")
    public ApiResponse<List<BusinessSummaryResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double rating,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<BusinessSummaryResponse> page = businessService.search(query, category, city, district, minPrice, maxPrice, rating, pageable);
        return ApiResponse.ofPage(page);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Business detail by slug")
    public ApiResponse<BusinessDetailResponse> detail(@PathVariable String slug) {
        return ApiResponse.ok(businessService.getBySlug(slug));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create business")
    public ApiResponse<BusinessSummaryResponse> create(@Valid @RequestBody CreateBusinessRequest request) {
        return ApiResponse.ok(businessService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update owned business")
    public ApiResponse<BusinessSummaryResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateBusinessRequest request) {
        return ApiResponse.ok(businessService.update(id, request));
    }

}
