package com.randevupazaryeri.employee.repository;

import com.randevupazaryeri.employee.entity.EmployeeInvitation;
import com.randevupazaryeri.employee.entity.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeInvitationRepository extends JpaRepository<EmployeeInvitation, UUID> {
    Optional<EmployeeInvitation> findByCode(String code);

    boolean existsByCode(String code);

    List<EmployeeInvitation> findByEmployeeIdAndStatus(UUID employeeId, InvitationStatus status);

    List<EmployeeInvitation> findByBusinessIdAndEmployeeIsNullAndStatusOrderByCreatedAtDesc(
            UUID businessId, InvitationStatus status);

    Optional<EmployeeInvitation> findByIdAndBusinessId(UUID id, UUID businessId);

    @Query("""
        select i from EmployeeInvitation i
        where lower(i.email) = lower(:email)
          and i.status = com.randevupazaryeri.employee.entity.InvitationStatus.PENDING
          and i.expiresAt > :now
        order by i.createdAt desc
        """)
    List<EmployeeInvitation> findPendingForEmail(@Param("email") String email, @Param("now") Instant now);
}
