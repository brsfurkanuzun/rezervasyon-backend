package com.randevupazaryeri.address.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserAddressResponse {
    private UUID id;
    private String label;
    private String recipientName;
    private String phone;
    private String addressLine;
    private String city;
    private String district;
    private String postalCode;
    @JsonProperty("isDefault")
    private boolean isDefault;
    private Instant createdAt;
    private Instant updatedAt;
}