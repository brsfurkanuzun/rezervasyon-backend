package com.randevupazaryeri.consent.entity;

import com.randevupazaryeri.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_consents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserConsent {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 32)
    private ConsentType consentType;

    @Column(nullable = false)
    private boolean granted;

    /** Where the decision was made, e.g. IOS_CUSTOMER or WEB_PARTNER. */
    @Column(length = 32)
    private String channel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
