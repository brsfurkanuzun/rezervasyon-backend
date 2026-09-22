package com.randevupazaryeri.favorite.repository;

import com.randevupazaryeri.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    boolean existsByCustomerIdAndBusinessId(UUID customerId, UUID businessId);
    Optional<Favorite> findByCustomerIdAndBusinessId(UUID customerId, UUID businessId);
    List<Favorite> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
