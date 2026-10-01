package com.randevupazaryeri.push.repository;

import com.randevupazaryeri.push.entity.DeviceToken;
import com.randevupazaryeri.push.entity.PushApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUserIdAndApp(UUID userId, PushApp app);

    @Modifying
    @Query("delete from DeviceToken d where d.token = :token and d.user.id = :userId")
    int deleteByTokenAndUserId(@Param("token") String token, @Param("userId") UUID userId);

    @Transactional
    @Modifying
    @Query("delete from DeviceToken d where d.token = :token")
    int deleteByToken(@Param("token") String token);
}
