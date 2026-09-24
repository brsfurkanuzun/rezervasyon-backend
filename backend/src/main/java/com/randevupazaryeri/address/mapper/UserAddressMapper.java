package com.randevupazaryeri.address.mapper;

import com.randevupazaryeri.address.dto.UserAddressResponse;
import com.randevupazaryeri.address.entity.UserAddress;

public final class UserAddressMapper {
    private UserAddressMapper() {}

    public static UserAddressResponse toResponse(UserAddress address) {
        return UserAddressResponse.builder()
                .id(address.getId())
                .label(address.getLabel())
                .recipientName(address.getRecipientName())
                .phone(address.getPhone())
                .addressLine(address.getAddressLine())
                .city(address.getCity())
                .district(address.getDistrict())
                .postalCode(address.getPostalCode())
                .isDefault(address.isDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}