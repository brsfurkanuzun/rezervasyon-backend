package com.randevupazaryeri.business.repository;

import com.randevupazaryeri.business.entity.Business;
import com.randevupazaryeri.business.entity.BusinessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID>, JpaSpecificationExecutor<Business> {
    Optional<Business> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Business> findByOwnerId(UUID ownerId);

    @Query("""
        select distinct b from Business b
        left join fetch b.categories
        where b.slug = :slug
        """)
    Optional<Business> findBySlugWithCategories(@Param("slug") String slug);
}
