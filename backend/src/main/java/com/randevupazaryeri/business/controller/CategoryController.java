package com.randevupazaryeri.business.controller;

import com.randevupazaryeri.business.dto.CategoryResponse;
import com.randevupazaryeri.business.service.CategoryService;
import com.randevupazaryeri.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories")
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "List active categories")
    public ApiResponse<List<CategoryResponse>> list() {
        return ApiResponse.ok(categoryService.listActive());
    }
}
