package com.randevupazaryeri.employee.repository;

import com.randevupazaryeri.employee.entity.TimeOff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TimeOffRepository extends JpaRepository<TimeOff, UUID> {
    List<TimeOff> findByEmployeeId(UUID employeeId);

    @Query("""
        select t from TimeOff t
        where t.employee.id = :employeeId
          and t.startAt < :end
          and t.endAt > :start
        """)
    List<TimeOff> findOverlapping(@Param("employeeId") UUID employeeId,
                                  @Param("start") Instant start,
                                  @Param("end") Instant end);
}
