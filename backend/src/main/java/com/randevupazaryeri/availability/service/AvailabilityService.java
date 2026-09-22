package com.randevupazaryeri.availability.service;

import com.randevupazaryeri.appointment.entity.AppointmentStatus;
import com.randevupazaryeri.appointment.repository.AppointmentRepository;
import com.randevupazaryeri.availability.dto.*;
import com.randevupazaryeri.business.entity.Business;
import com.randevupazaryeri.business.repository.BusinessRepository;
import com.randevupazaryeri.common.exception.ResourceNotFoundException;
import com.randevupazaryeri.employee.entity.Employee;
import com.randevupazaryeri.employee.entity.TimeOff;
import com.randevupazaryeri.employee.entity.WorkingHour;
import com.randevupazaryeri.employee.repository.EmployeeRepository;
import com.randevupazaryeri.employee.repository.TimeOffRepository;
import com.randevupazaryeri.employee.repository.WorkingHourRepository;
import com.randevupazaryeri.serviceoffer.entity.ServiceOffer;
import com.randevupazaryeri.serviceoffer.repository.ServiceOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final BusinessRepository businessRepository;
    private final ServiceOfferRepository serviceOfferRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkingHourRepository workingHourRepository;
    private final TimeOffRepository timeOffRepository;
    private final AppointmentRepository appointmentRepository;

    public AvailabilityResponse getAvailability(UUID businessId, UUID serviceId, UUID employeeId, LocalDate date) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        ServiceOffer service = serviceOfferRepository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        List<Employee> employees;
        if (employeeId != null) {
            Employee e = employeeRepository.findByIdAndBusinessId(employeeId, businessId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
            if (!employeeRepository.providesService(employeeId, serviceId)) {
                throw new ResourceNotFoundException("Employee does not provide this service");
            }
            employees = List.of(e);
        } else {
            employees = employeeRepository.findActiveProvidingService(businessId, serviceId);
        }

        ZoneId zone = ZoneId.of(business.getTimezone());
        int dayOfWeek = date.getDayOfWeek().getValue();
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();

        List<EmployeeAvailabilityResponse> result = new ArrayList<>();
        for (Employee employee : employees) {
            List<WorkingHour> hours = workingHourRepository
                    .findByEmployeeIdAndDayOfWeekAndIsAvailableTrue(employee.getId(), dayOfWeek);
            List<TimeOff> timeOffs = timeOffRepository.findOverlapping(employee.getId(), dayStart, dayEnd);
            var appointments = appointmentRepository.findActiveOverlapping(employee.getId(), dayStart, dayEnd);

            List<TimeSlotResponse> slots = new ArrayList<>();
            for (WorkingHour wh : hours) {
                LocalDateTime windowStart = LocalDateTime.of(date, wh.getStartTime());
                LocalDateTime windowEnd = LocalDateTime.of(date, wh.getEndTime());
                LocalDateTime cursor = windowStart;
                Duration duration = Duration.ofMinutes(service.getDurationMinutes());
                while (!cursor.plus(duration).isAfter(windowEnd)) {
                    Instant start = cursor.atZone(zone).toInstant();
                    Instant end = cursor.plus(duration).atZone(zone).toInstant();
                    boolean available = !overlapsTimeOff(start, end, timeOffs)
                            && !overlapsAppointment(start, end, appointments)
                            && start.isAfter(Instant.now());
                    slots.add(TimeSlotResponse.builder()
                            .start(cursor.format(HM))
                            .end(cursor.plus(duration).format(HM))
                            .available(available)
                            .build());
                    cursor = cursor.plus(duration);
                }
            }
            result.add(EmployeeAvailabilityResponse.builder()
                    .employeeId(employee.getId())
                    .employeeName(employee.getFirstName() + " " + employee.getLastName())
                    .slots(slots)
                    .build());
        }
        return AvailabilityResponse.builder().date(date).employees(result).build();
    }

    private boolean overlapsTimeOff(Instant start, Instant end, List<TimeOff> timeOffs) {
        return timeOffs.stream().anyMatch(t -> t.getStartAt().isBefore(end) && t.getEndAt().isAfter(start));
    }

    private boolean overlapsAppointment(Instant start, Instant end,
                                        List<com.randevupazaryeri.appointment.entity.Appointment> appointments) {
        return appointments.stream().anyMatch(a -> a.getStartDateTime().isBefore(end) && a.getEndDateTime().isAfter(start));
    }
}
