package com.randevupazaryeri.auth.dto;

import com.randevupazaryeri.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    @NotBlank
    @Size(max = 8192)
    private String idToken;

    /** Role for a newly created account; defaults to CUSTOMER. */
    private Role role;
}
