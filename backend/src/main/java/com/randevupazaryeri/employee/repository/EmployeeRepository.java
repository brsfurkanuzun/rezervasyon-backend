package com.randevupazaryeri.employee.repository;

import com.randevupazaryeri.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    List<Employee> findByBusinessIdAndIsActiveTrue(UUID businessId);
    List<Employee> findByBusinessId(UUID businessId);
    Optional<Employee> findByIdAndBusinessId(UUID id, UUID businessId);
    List<Employee> findByUserIdAndIsActiveTrue(UUID userId);
    Optional<Employee> findByBusinessIdAndUserIdAndIsActiveTrue(UUID businessId, UUID userId);
    boolean existsByBusinessIdAndUserId(UUID businessId, UUID userId);

    /**
     * Linked expert profiles show the account photo: those without a photo, or still on the
     * {@code previous} account photo, switch to {@code url}. A photo the owner set separately is kept.
     * Must run inside a transaction.
     */
    default void followAccountPhoto(UUID userId, String previous, String url) {
        for (Employee employee : findByUserIdAndIsActiveTrue(userId)) {
            if (employee.getPhotoUrl() == null || Objects.equals(employee.getPhotoUrl(), previous)) {
                employee.setPhotoUrl(url);
            }
        }
    }

    @Query("""
        select e from Employee e join e.services s
        where e.business.id = :businessId and e.isActive = true and s.id = :serviceId and s.isActive = true
        """)
    List<Employee> findActiveProvidingService(@Param("businessId") UUID businessId, @Param("serviceId") UUID serviceId);

    @Query("""
        select case when count(e) > 0 then true else false end from Employee e join e.services s
        where e.id = :employeeId and s.id = :serviceId
        """)
    boolean providesService(@Param("employeeId") UUID employeeId, @Param("serviceId") UUID serviceId);
}
