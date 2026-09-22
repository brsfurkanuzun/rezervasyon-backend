package com.randevupazaryeri.appointment.repository;

import com.randevupazaryeri.appointment.entity.Appointment;
import com.randevupazaryeri.appointment.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Page<Appointment> findByCustomerIdOrderByStartDateTimeDesc(UUID customerId, Pageable pageable);

    Page<Appointment> findByBusinessIdOrderByStartDateTimeDesc(UUID businessId, Pageable pageable);

    @Query("""
        select a from Appointment a
        where a.employee.id = :employeeId
          and a.status in (com.randevupazaryeri.appointment.entity.AppointmentStatus.PENDING,
                           com.randevupazaryeri.appointment.entity.AppointmentStatus.CONFIRMED)
          and a.startDateTime < :end
          and a.endDateTime > :start
        """)
    List<Appointment> findActiveOverlapping(@Param("employeeId") UUID employeeId,
                                            @Param("start") Instant start,
                                            @Param("end") Instant end);
}
