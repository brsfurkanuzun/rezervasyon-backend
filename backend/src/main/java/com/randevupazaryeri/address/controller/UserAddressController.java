package com.randevupazaryeri.address.controller;

import com.randevupazaryeri.address.dto.SaveUserAddressRequest;
import com.randevupazaryeri.address.dto.UserAddressResponse;
import com.randevupazaryeri.address.service.UserAddressService;
import com.randevupazaryeri.common.dto.ApiResponse;
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
@RequestMapping("/api/v1/account/addresses")
@RequiredArgsConstructor
@Tag(name = "Account addresses")
@SecurityRequirement(name = "bearerAuth")
public class UserAddressController {
    private final UserAddressService service;

    @GetMapping
    @Operation(summary = "List my saved addresses")
    public ApiResponse<List<UserAddressResponse>> list() {
        return ApiResponse.ok(service.listMine());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a saved address")
    public ApiResponse<UserAddressResponse> create(@Valid @RequestBody SaveUserAddressRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a saved address")
    public ApiResponse<UserAddressResponse> update(@PathVariable UUID id, @Valid @RequestBody SaveUserAddressRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a saved address")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}