package com.randevupazaryeri.business.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** A business the current provider works at, either as its owner or as staff. */
@Data @Builder
public class WorkplaceResponse {
    public enum Role { OWNER, STAFF }

    private BusinessSummaryResponse business;
    private Role role;
    /** The caller's employee record in this business, if linked (always set for STAFF). */
    private UUID employeeId;
}
