package com.randevupazaryeri.push.dto;

import com.randevupazaryeri.push.entity.PushApp;
import com.randevupazaryeri.push.entity.PushEnvironment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterDeviceRequest {
    @NotBlank
    @Pattern(regexp = "^[0-9a-fA-F]{32,200}$", message = "must be a hex APNs device token")
    private String token;

    @NotNull
    private PushApp app;

    @NotNull
    private PushEnvironment environment;
}
