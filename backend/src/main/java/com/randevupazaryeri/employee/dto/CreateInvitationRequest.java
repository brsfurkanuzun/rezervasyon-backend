package com.randevupazaryeri.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateInvitationRequest {
    /** Optional: the invitation also shows up in-app for the account with this email. */
    @Email
    @Size(max = 255)
    private String email;
}
