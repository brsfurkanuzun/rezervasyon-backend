package com.randevupazaryeri.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    /** Required only when the account already has a password the user chose. */
    @Size(max = 100)
    private String currentPassword;

    @NotBlank
    @Size(min = 8, max = 100)
    private String newPassword;
}
