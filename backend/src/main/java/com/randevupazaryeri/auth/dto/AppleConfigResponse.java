package com.randevupazaryeri.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppleConfigResponse {
    /** Public Services ID for Sign in with Apple on the web; absent while it is disabled. */
    private String clientId;
}
