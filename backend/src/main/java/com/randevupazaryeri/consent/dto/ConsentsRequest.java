package com.randevupazaryeri.consent.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** Sign-up consents. Unset fields are not recorded; marketing is recorded whether granted or declined. */
@Data
public class ConsentsRequest {
    private Boolean termsAccepted;
    private Boolean partnerTermsAccepted;
    private Boolean kvkkAcknowledged;
    private Boolean privacyAcknowledged;
    private Boolean marketingConsent;

    @Pattern(regexp = "[A-Z_]{1,32}")
    private String channel;
}
