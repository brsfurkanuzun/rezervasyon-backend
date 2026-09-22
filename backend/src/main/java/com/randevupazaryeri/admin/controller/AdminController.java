package com.randevupazaryeri.admin.controller;

import com.randevupazaryeri.business.dto.BusinessSummaryResponse;
import com.randevupazaryeri.business.dto.CategoryResponse;
import com.randevupazaryeri.business.entity.BusinessStatus;
import com.randevupazaryeri.business.service.BusinessService;
import com.randevupazaryeri.business.service.CategoryService;
import com.randevupazaryeri.common.dto.ApiResponse;
import com.randevupazaryeri.common.exception.ResourceNotFoundException;
import com.randevupazaryeri.user.dto.UserResponse;
import com.randevupazaryeri.user.mapper.UserMapper;
import com.randevupazaryeri.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
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
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final BusinessService businessService;
    private final CategoryService categoryService;

    @GetMapping("/users")
    @Operation(summary = "List users")
    public ApiResponse<List<UserResponse>> users(@PageableDefault(size = 50) Pageable pageable) {
        Page<UserResponse> page = userRepository.findAll(pageable).map(UserMapper::toResponse);
        return ApiResponse.ofPage(page);
    }

    @PatchMapping("/users/{id}/active")
    @Operation(summary = "Activate or deactivate user")
    public ApiResponse<UserResponse> setActive(@PathVariable UUID id, @RequestParam boolean active) {
        var user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(active);
        userRepository.save(user);
        return ApiResponse.ok(UserMapper.toResponse(user));
    }

    @PatchMapping("/businesses/{id}/status")
    @Operation(summary = "Update business status (approve/suspend)")
    public ApiResponse<BusinessSummaryResponse> businessStatus(@PathVariable UUID id, @RequestParam BusinessStatus status) {
        return ApiResponse.ok(businessService.updateStatus(id, status));
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create category")
    public ApiResponse<CategoryResponse> createCategory(@RequestBody CreateCategoryBody body) {
        return ApiResponse.ok(categoryService.create(body.getCode(), body.getName(), body.getDescription()));
    }

    @PatchMapping("/categories/{id}")
    @Operation(summary = "Update category")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable UUID id, @RequestBody UpdateCategoryBody body) {
        return ApiResponse.ok(categoryService.update(id, body.getName(), body.getDescription(), body.getActive()));
    }

    @Data
    public static class CreateCategoryBody {
        @NotBlank private String code;
        @NotBlank private String name;
        private String description;
    }

    @Data
    public static class UpdateCategoryBody {
        private String name;
        private String description;
        private Boolean active;
    }
}
