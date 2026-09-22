package com.randevupazaryeri.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailLookupRequest {
    @NotBlank
    @Email
    private String email;
}
