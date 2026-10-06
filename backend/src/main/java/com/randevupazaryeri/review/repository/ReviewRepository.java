package com.randevupazaryeri.review.repository;

import com.randevupazaryeri.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    boolean existsByAppointmentId(UUID appointmentId);
    boolean existsByCustomerIdAndBusinessId(UUID customerId, UUID businessId);
    long countByBusinessId(UUID businessId);
    List<Review> findTop5ByBusinessIdOrderByCreatedAtDesc(UUID businessId);
    Page<Review> findByBusinessIdOrderByCreatedAtDesc(UUID businessId, Pageable pageable);

    @Query("""
            select r from Review r
            where r.business.id = :businessId
              and r.id not in :hiddenReviewIds
              and r.customer.id not in :blockedAuthorIds
            order by r.createdAt desc
            """)
    Page<Review> findVisibleByBusinessId(@Param("businessId") UUID businessId,
                                         @Param("hiddenReviewIds") Collection<UUID> hiddenReviewIds,
                                         @Param("blockedAuthorIds") Collection<UUID> blockedAuthorIds,
                                         Pageable pageable);

    @Query("select avg(r.rating) from Review r where r.business.id = :businessId")
    Double averageRatingByBusinessId(@Param("businessId") UUID businessId);

    @Query("select avg(r.rating) from Review r where r.appointment.employee.id = :employeeId")
    Double averageRatingByEmployeeId(@Param("employeeId") UUID employeeId);

    @Query("select count(r) from Review r where r.appointment.employee.id = :employeeId")
    long countByEmployeeId(@Param("employeeId") UUID employeeId);

    @Modifying
    @Query("delete from Review r where r.business.id = :businessId")
    int deleteByBusinessId(@Param("businessId") UUID businessId);
}
