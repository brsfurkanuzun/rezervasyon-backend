package com.randevupazaryeri.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank
    @Size(max = 100)
    private String token;

    @NotBlank
    @Size(min = 8, max = 100)
    private String newPassword;
}
