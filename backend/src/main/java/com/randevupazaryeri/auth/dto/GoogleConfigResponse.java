package com.randevupazaryeri.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleConfigResponse {
    /** Public OAuth client id for the Google sign-in button; absent while Google sign-in is disabled. */
    private String clientId;
}
