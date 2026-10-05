package com.randevupazaryeri.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "passwordHash")
public class User {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 32)
    private String gender;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** False for social sign-ups, whose password hash is random and unknown to the user. */
    @Column(name = "password_set", nullable = false)
    @Builder.Default
    private boolean passwordSet = true;

    /** Stable "sub" claim from Sign in with Apple. */
    @Column(name = "apple_user_id")
    private String appleUserId;

    /** Stable "sub" claim from Google Sign-In. */
    @Column(name = "google_user_id")
    private String googleUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
