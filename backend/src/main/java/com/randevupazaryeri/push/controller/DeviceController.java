package com.randevupazaryeri.push.controller;

import com.randevupazaryeri.push.dto.RegisterDeviceRequest;
import com.randevupazaryeri.push.service.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "Devices")
@SecurityRequirement(name = "bearerAuth")
public class DeviceController {
    private final DeviceTokenService deviceTokenService;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Register this device for push notifications")
    public void register(@Valid @RequestBody RegisterDeviceRequest request) {
        deviceTokenService.register(request);
    }

    @DeleteMapping("/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Stop push notifications for this device")
    public void unregister(@PathVariable String token) {
        deviceTokenService.unregister(token);
    }
}
