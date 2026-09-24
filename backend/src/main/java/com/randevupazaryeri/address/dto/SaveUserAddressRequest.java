package com.randevupazaryeri.address.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SaveUserAddressRequest {
    @NotBlank private String label;
    @NotBlank private String recipientName;
    private String phone;
    @NotBlank private String addressLine;
    @NotBlank private String city;
    private String district;
    private String postalCode;
    @JsonProperty("isDefault")
    private boolean isDefault;
}