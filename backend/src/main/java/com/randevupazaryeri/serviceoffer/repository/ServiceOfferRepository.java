package com.randevupazaryeri.serviceoffer.repository;

import com.randevupazaryeri.serviceoffer.entity.ServiceOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOfferRepository extends JpaRepository<ServiceOffer, UUID> {
    List<ServiceOffer> findByBusinessIdAndIsActiveTrue(UUID businessId);
    List<ServiceOffer> findByBusinessId(UUID businessId);
    Optional<ServiceOffer> findByIdAndBusinessId(UUID id, UUID businessId);

    @Query("select min(s.price) from ServiceOffer s where s.business.id = :businessId and s.isActive = true")
    Optional<BigDecimal> findMinActivePriceByBusinessId(@Param("businessId") UUID businessId);
}
