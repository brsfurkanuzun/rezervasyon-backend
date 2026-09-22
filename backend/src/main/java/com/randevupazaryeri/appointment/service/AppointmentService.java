package com.randevupazaryeri.appointment.service;

import com.randevupazaryeri.appointment.dto.AppointmentResponse;
import com.randevupazaryeri.appointment.dto.CancelAppointmentRequest;
import com.randevupazaryeri.appointment.dto.CreateAppointmentRequest;
import com.randevupazaryeri.appointment.entity.Appointment;
import com.randevupazaryeri.appointment.entity.AppointmentStatus;
import com.randevupazaryeri.appointment.mapper.AppointmentMapper;
import com.randevupazaryeri.appointment.repository.AppointmentRepository;
import com.randevupazaryeri.business.entity.Business;
import com.randevupazaryeri.business.entity.BusinessStatus;
import com.randevupazaryeri.business.service.BusinessOwnershipService;
import com.randevupazaryeri.common.exception.*;
import com.randevupazaryeri.common.security.SecurityUtils;
import com.randevupazaryeri.config.AppointmentProperties;
import com.randevupazaryeri.employee.entity.Employee;
import com.randevupazaryeri.employee.entity.WorkingHour;
import com.randevupazaryeri.employee.repository.EmployeeRepository;
import com.randevupazaryeri.employee.repository.TimeOffRepository;
import com.randevupazaryeri.employee.repository.WorkingHourRepository;
import com.randevupazaryeri.notification.service.NotificationService;
import com.randevupazaryeri.serviceoffer.entity.ServiceOffer;
import com.randevupazaryeri.serviceoffer.repository.ServiceOfferRepository;
import com.randevupazaryeri.user.entity.Role;
import com.randevupazaryeri.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final BusinessOwnershipService ownershipService;
    private final ServiceOfferRepository serviceOfferRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkingHourRepository workingHourRepository;
    private final TimeOffRepository timeOffRepository;
    private final UserService userService;
    private final AppointmentProperties appointmentProperties;
    private final NotificationService notificationService;

    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        var principal = SecurityUtils.currentPrincipal();
        if (principal.getRole() != Role.CUSTOMER && principal.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only customers can book appointments");
        }
        Business business = ownershipService.getBusiness(request.getBusinessId());
        if (business.getStatus() != BusinessStatus.ACTIVE) {
            throw new InvalidAppointmentException("Business is not active");
        }
        ServiceOffer service = serviceOfferRepository.findByIdAndBusinessId(request.getServiceId(), business.getId())
                .orElseThrow(() -> new InvalidAppointmentException("Service not found in business"));
        if (!service.isActive()) {
            throw new InvalidAppointmentException("Service is inactive");
        }
        Employee employee = employeeRepository.findByIdAndBusinessId(request.getEmployeeId(), business.getId())
                .orElseThrow(() -> new InvalidAppointmentException("Employee not found in business"));
        if (!employee.isActive()) {
            throw new InvalidAppointmentException("Employee is inactive");
        }
        if (!employeeRepository.providesService(employee.getId(), service.getId())) {
            throw new InvalidAppointmentException("Employee does not provide this service");
        }

        Instant start = request.getStartDateTime();
        Instant end = start.plus(Duration.ofMinutes(service.getDurationMinutes()));
        if (!start.isAfter(Instant.now())) {
            throw new InvalidAppointmentException("Cannot book appointments in the past");
        }
        validateWorkingHours(business, employee, start, end);
        if (!timeOffRepository.findOverlapping(employee.getId(), start, end).isEmpty()) {
            throw new InvalidAppointmentException("Employee is not available (time off)");
        }
        if (!appointmentRepository.findActiveOverlapping(employee.getId(), start, end).isEmpty()) {
            throw new AppointmentConflictException();
        }

        AppointmentStatus status = business.isAutoConfirm() ? AppointmentStatus.CONFIRMED : AppointmentStatus.PENDING;
        Appointment appointment = Appointment.builder()
                .customer(userService.getById(principal.getId()))
                .business(business)
                .employee(employee)
                .service(service)
                .startDateTime(start)
                .endDateTime(end)
                .status(status)
                .price(service.getPrice())
                .customerNote(request.getCustomerNote())
                .build();
        try {
            appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException ex) {
            throw new AppointmentConflictException();
        }

        notificationService.notifyUser(business.getOwner().getId(), "APPOINTMENT_CREATED",
                "New appointment", "A new appointment was booked at " + business.getName());
        notificationService.notifyUser(principal.getId(), "APPOINTMENT_CREATED",
                "Appointment booked", "Your appointment is " + status.name());

        return AppointmentMapper.toResponse(appointment);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> myAppointments(Pageable pageable) {
        return appointmentRepository.findByCustomerIdOrderByStartDateTimeDesc(SecurityUtils.currentUserId(), pageable)
                .map(AppointmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> businessAppointments(UUID businessId, Pageable pageable) {
        ownershipService.requireOwnedBusiness(businessId);
        return appointmentRepository.findByBusinessIdOrderByStartDateTimeDesc(businessId, pageable)
                .map(AppointmentMapper::toResponse);
    }

    @Transactional
    public AppointmentResponse cancel(UUID id, CancelAppointmentRequest request) {
        Appointment appointment = getAppointment(id);
        var principal = SecurityUtils.currentPrincipal();
        boolean isCustomer = appointment.getCustomer().getId().equals(principal.getId());
        boolean isOwner = appointment.getBusiness().getOwner().getId().equals(principal.getId());
        boolean isAdmin = principal.getRole() == Role.ADMIN;

        if (!isCustomer && !isOwner && !isAdmin) {
            throw new ForbiddenException("Not allowed to cancel this appointment");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.COMPLETED
                || appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new InvalidAppointmentException("Appointment cannot be cancelled");
        }
        if (isCustomer && !isAdmin) {
            Instant deadline = appointment.getStartDateTime()
                    .minus(Duration.ofHours(appointmentProperties.getCustomerCancellationDeadlineHours()));
            if (Instant.now().isAfter(deadline)) {
                throw new InvalidAppointmentException("Cancellation window has closed");
            }
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(request != null ? request.getReason() : null);
        return AppointmentMapper.toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse confirm(UUID businessId, UUID id) {
        ownershipService.requireOwnedBusiness(businessId);
        Appointment appointment = getInBusiness(id, businessId);
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new InvalidAppointmentException("Only pending appointments can be confirmed");
        }
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        notificationService.notifyUser(appointment.getCustomer().getId(), "APPOINTMENT_CONFIRMED",
                "Appointment confirmed", "Your appointment was confirmed");
        return AppointmentMapper.toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse complete(UUID businessId, UUID id) {
        ownershipService.requireOwnedBusiness(businessId);
        Appointment appointment = getInBusiness(id, businessId);
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED && appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new InvalidAppointmentException("Appointment cannot be completed");
        }
        if (appointment.getStartDateTime().isAfter(Instant.now())) {
            throw new InvalidAppointmentException("Cannot complete a future appointment");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return AppointmentMapper.toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse noShow(UUID businessId, UUID id) {
        ownershipService.requireOwnedBusiness(businessId);
        Appointment appointment = getInBusiness(id, businessId);
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED && appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new InvalidAppointmentException("Appointment cannot be marked no-show");
        }
        appointment.setStatus(AppointmentStatus.NO_SHOW);
        return AppointmentMapper.toResponse(appointment);
    }

    private void validateWorkingHours(Business business, Employee employee, Instant start, Instant end) {
        ZoneId zone = ZoneId.of(business.getTimezone());
        ZonedDateTime zStart = start.atZone(zone);
        ZonedDateTime zEnd = end.atZone(zone);
        if (!zStart.toLocalDate().equals(zEnd.toLocalDate())) {
            throw new InvalidAppointmentException("Appointments must stay within a single day");
        }
        int dow = zStart.getDayOfWeek().getValue();
        List<WorkingHour> hours = workingHourRepository.findByEmployeeIdAndDayOfWeekAndIsAvailableTrue(employee.getId(), dow);
        LocalTime st = zStart.toLocalTime();
        LocalTime et = zEnd.toLocalTime();
        boolean ok = hours.stream().anyMatch(h -> !st.isBefore(h.getStartTime()) && !et.isAfter(h.getEndTime()));
        if (!ok) {
            throw new InvalidAppointmentException("Outside employee working hours");
        }
    }

    private Appointment getAppointment(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    }

    private Appointment getInBusiness(UUID id, UUID businessId) {
        Appointment a = getAppointment(id);
        if (!a.getBusiness().getId().equals(businessId)) {
            throw new ResourceNotFoundException("Appointment not found");
        }
        return a;
    }
}
