package com.randevupazaryeri.user.mapper;

import com.randevupazaryeri.user.dto.UserResponse;
import com.randevupazaryeri.user.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .photoUrl(user.getPhotoUrl())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .role(user.getRole())
                .isActive(user.isActive())
                .hasPassword(user.isPasswordSet())
                .googleLinked(user.getGoogleUserId() != null)
                .appleLinked(user.getAppleUserId() != null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
