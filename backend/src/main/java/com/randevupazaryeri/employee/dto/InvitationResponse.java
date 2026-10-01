package com.randevupazaryeri.employee.dto;

import com.randevupazaryeri.employee.entity.InvitationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class InvitationResponse {
    private UUID id;
    /** Human-friendly code, e.g. "ABCD-2345". */
    private String code;
    /** Deep link that opens the partner app on the join screen. */
    private String link;
    private String email;
    private InvitationStatus status;
    private Instant expiresAt;
    private UUID businessId;
    private String businessName;
    private String businessLogoUrl;
    private UUID employeeId;
    private String employeeName;
    private String employeeTitle;
}
