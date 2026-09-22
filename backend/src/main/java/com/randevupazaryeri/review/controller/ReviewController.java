package com.randevupazaryeri.review.controller;

import com.randevupazaryeri.common.dto.ApiResponse;
import com.randevupazaryeri.review.dto.CreateReviewRequest;
import com.randevupazaryeri.review.dto.ReviewResponse;
import com.randevupazaryeri.review.service.ReviewService;
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
@RequiredArgsConstructor
@Tag(name = "Reviews")
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/api/v1/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create review for completed appointment")
    public ApiResponse<ReviewResponse> create(@Valid @RequestBody CreateReviewRequest request) {
        return ApiResponse.ok(reviewService.create(request));
    }

    @GetMapping("/api/v1/businesses/{businessId}/reviews")
    @Operation(summary = "List business reviews")
    public ApiResponse<List<ReviewResponse>> list(@PathVariable UUID businessId, @PageableDefault(size = 20) Pageable pageable) {
        Page<ReviewResponse> page = reviewService.listByBusiness(businessId, pageable);
        return ApiResponse.ofPage(page);
    }
}
