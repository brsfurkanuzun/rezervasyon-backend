package com.randevupazaryeri.address.repository;

import com.randevupazaryeri.address.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {
    List<UserAddress> findByUserIdOrderByIsDefaultDescUpdatedAtDesc(UUID userId);
    Optional<UserAddress> findByIdAndUserId(UUID id, UUID userId);
}