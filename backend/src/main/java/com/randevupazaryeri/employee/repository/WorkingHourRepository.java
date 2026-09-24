package com.randevupazaryeri.employee.repository;

import com.randevupazaryeri.employee.entity.WorkingHour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkingHourRepository extends JpaRepository<WorkingHour, UUID> {
    List<WorkingHour> findByEmployeeIdAndDayOfWeekAndIsAvailableTrue(UUID employeeId, int dayOfWeek);
    List<WorkingHour> findByEmployeeId(UUID employeeId);
    List<WorkingHour> findByEmployeeBusinessIdAndEmployeeIsActiveTrueAndIsAvailableTrue(UUID businessId);
    void deleteByEmployeeId(UUID employeeId);
}
