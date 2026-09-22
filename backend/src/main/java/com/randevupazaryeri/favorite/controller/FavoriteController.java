package com.randevupazaryeri.favorite.controller;

import com.randevupazaryeri.common.dto.ApiResponse;
import com.randevupazaryeri.favorite.dto.FavoriteResponse;
import com.randevupazaryeri.favorite.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('CUSTOMER')")
public class FavoriteController {
    private final FavoriteService favoriteService;

    @PostMapping("/{businessId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Favorite a business")
    public ApiResponse<FavoriteResponse> add(@PathVariable UUID businessId) {
        return ApiResponse.ok(favoriteService.add(businessId));
    }

    @DeleteMapping("/{businessId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove favorite")
    public void remove(@PathVariable UUID businessId) {
        favoriteService.remove(businessId);
    }

    @GetMapping
    @Operation(summary = "List my favorites")
    public ApiResponse<List<FavoriteResponse>> list() {
        return ApiResponse.ok(favoriteService.listMine());
    }
}
