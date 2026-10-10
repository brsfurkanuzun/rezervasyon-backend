package com.randevupazaryeri.consent.entity;

import com.randevupazaryeri.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_notice_receipts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserNoticeReceipt {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notice_type", nullable = false, length = 32)
    private NoticeType noticeType;

    @Column(name = "document_version", nullable = false, length = 64)
    private String documentVersion;

    @Column(nullable = false, length = 32)
    private String channel;

    @Column(name = "presented_at", nullable = false)
    private Instant presentedAt;

    @PrePersist
    void onCreate() {
        if (presentedAt == null) {
            presentedAt = Instant.now();
        }
    }
}
