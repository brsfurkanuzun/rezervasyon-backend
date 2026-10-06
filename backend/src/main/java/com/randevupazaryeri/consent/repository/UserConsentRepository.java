package com.randevupazaryeri.consent.repository;

import com.randevupazaryeri.consent.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {
    List<UserConsent> findByUserIdOrderByCreatedAtAsc(UUID userId);
}
