package com.randevupazaryeri.user.dto;

import com.randevupazaryeri.user.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String photoUrl;
    private LocalDate birthDate;
    private String gender;
    private Role role;
    private boolean isActive;
    private Instant createdAt;
}
