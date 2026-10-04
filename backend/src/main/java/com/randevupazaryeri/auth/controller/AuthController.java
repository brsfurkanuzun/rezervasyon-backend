package com.randevupazaryeri.auth.controller;

import com.randevupazaryeri.auth.dto.*;
import com.randevupazaryeri.auth.service.AuthService;
import com.randevupazaryeri.common.dto.ApiResponse;
import com.randevupazaryeri.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register customer or provider")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/apple")
    @Operation(summary = "Sign in (or sign up) with an Apple identity token")
    public ApiResponse<AuthResponse> apple(@Valid @RequestBody AppleLoginRequest request) {
        return ApiResponse.ok(authService.loginWithApple(request));
    }

    @GetMapping("/apple/config")
    @Operation(summary = "Public Sign in with Apple configuration for the websites")
    public ApiResponse<AppleConfigResponse> appleConfig() {
        return ApiResponse.ok(authService.appleConfig());
    }

    @PostMapping("/google")
    @Operation(summary = "Sign in (or sign up) with a Google ID token")
    public ApiResponse<AuthResponse> google(@Valid @RequestBody GoogleLoginRequest request) {
        return ApiResponse.ok(authService.loginWithGoogle(request));
    }

    @GetMapping("/google/config")
    @Operation(summary = "Public Google sign-in configuration for the websites")
    public ApiResponse<GoogleConfigResponse> googleConfig() {
        return ApiResponse.ok(authService.googleConfig());
    }

    @PostMapping("/check-email")
    @Operation(summary = "Check whether an email is already registered")
    public ApiResponse<EmailLookupResponse> checkEmail(@Valid @RequestBody EmailLookupRequest request) {
        return ApiResponse.ok(authService.lookupEmail(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/upgrade-to-provider")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Turn my customer account into a business account (keeps customer features)")
    public ApiResponse<AuthResponse> upgradeToProvider() {
        return ApiResponse.ok(authService.upgradeToProvider());
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout and revoke refresh tokens")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.empty();
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Current authenticated user")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.ok(authService.me());
    }

    @PutMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update current authenticated user")
    public ApiResponse<UserResponse> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(authService.updateProfile(request));
    }
}
