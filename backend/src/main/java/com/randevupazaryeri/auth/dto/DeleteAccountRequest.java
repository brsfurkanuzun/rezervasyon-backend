package com.randevupazaryeri.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeleteAccountRequest {
    /** Required when the account has a password; social-only accounts confirm in the app instead. */
    @Size(max = 100)
    private String password;
}
