package com.randevupazaryeri.auth.repository;

import com.randevupazaryeri.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndCreatedAtAfter(UUID userId, Instant after);

    @Modifying
    @Query("delete from PasswordResetToken t where t.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from PasswordResetToken t where t.userId = :userId and t.createdAt < :before")
    int deleteOlderThan(@Param("userId") UUID userId, @Param("before") Instant before);
}
