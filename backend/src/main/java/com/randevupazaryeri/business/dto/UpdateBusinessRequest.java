package com.randevupazaryeri.business.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateBusinessRequest {
    @Size(max = 200)
    private String name;
    private String description;
    private String phone;
    private String email;
    private String address;
    private String city;
    private String district;
    private Double latitude;
    private Double longitude;
    private String logoUrl;
    private String coverImageUrl;
    private String timezone;
    private Boolean autoConfirm;
    private List<UUID> categoryIds;
}
