package com.randevupazaryeri.auth.dto;

import com.randevupazaryeri.consent.dto.ConsentsRequest;
import com.randevupazaryeri.user.entity.Role;
import jakarta.validation.Valid;
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

    /** Recorded only when this sign-in creates the account. */
    @Valid
    private ConsentsRequest consents;
}
