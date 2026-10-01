package com.randevupazaryeri.employee.entity;

import com.randevupazaryeri.business.entity.Business;
import com.randevupazaryeri.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/** Owner-issued invitation that links a user account to an employee (staff) record. */
@Entity
@Table(name = "employee_invitations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeInvitation {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, length = 16)
    private String code;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvitationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by")
    private User acceptedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public boolean isUsable() {
        return status == InvitationStatus.PENDING && expiresAt.isAfter(Instant.now());
    }
}
