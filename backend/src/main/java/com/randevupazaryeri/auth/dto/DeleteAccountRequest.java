package com.randevupazaryeri.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeleteAccountRequest {
    /** Required when the account has a password; social-only accounts confirm in the app instead. */
    @Size(max = 100)
    private String password;

    /** Fresh Sign in with Apple authorization code; used once to revoke the account's Apple tokens. */
    @Size(max = 2000)
    private String appleAuthorizationCode;

    /** Bundle id of the app that produced {@link #appleAuthorizationCode}. */
    @Size(max = 100)
    private String appleClientId;
}
