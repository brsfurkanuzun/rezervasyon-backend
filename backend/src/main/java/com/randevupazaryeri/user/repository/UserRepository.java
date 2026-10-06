package com.randevupazaryeri.user.repository;

import com.randevupazaryeri.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);
    Optional<User> findByAppleUserId(String appleUserId);
    Optional<User> findByGoogleUserId(String googleUserId);
}
