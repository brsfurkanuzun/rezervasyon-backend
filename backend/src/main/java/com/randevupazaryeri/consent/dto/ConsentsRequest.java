package com.randevupazaryeri.consent.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Sign-up decisions; KVKK/privacy versions identify notices presented, not consent to those notices. */
@Data
public class ConsentsRequest {
    private Boolean termsAccepted;
    private Boolean partnerTermsAccepted;
    private Boolean kvkkAcknowledged;
    private Boolean privacyAcknowledged;
    private Boolean marketingConsent;

    @Size(max = 64)
    private String termsVersion;

    @Size(max = 64)
    private String partnerTermsVersion;

    @Size(max = 64)
    private String kvkkNoticeVersion;

    @Size(max = 64)
    private String privacyPolicyVersion;

    @Size(max = 64)
    private String marketingConsentVersion;

    @Pattern(regexp = "[A-Z_]{1,32}")
    private String channel;
}
