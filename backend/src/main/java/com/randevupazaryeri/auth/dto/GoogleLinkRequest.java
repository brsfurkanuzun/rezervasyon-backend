package com.randevupazaryeri.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GoogleLinkRequest {
    @NotBlank
    @Size(max = 8192)
    private String idToken;
}
