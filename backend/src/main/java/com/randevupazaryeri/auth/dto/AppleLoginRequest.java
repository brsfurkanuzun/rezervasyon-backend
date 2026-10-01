package com.randevupazaryeri.auth.dto;

import com.randevupazaryeri.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AppleLoginRequest {
    @NotBlank
    private String identityToken;

    /** Apple only shares the name on the very first authorization. */
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    /** Role for a newly created account; defaults to CUSTOMER. */
    private Role role;
}
