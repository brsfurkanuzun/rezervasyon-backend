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
import com.randevupazaryeri.push.entity.PushApp;
import com.randevupazaryeri.serviceoffer.entity.ServiceOffer;
import com.randevupazaryeri.serviceoffer.repository.ServiceOfferRepository;
import com.randevupazaryeri.user.entity.Role;
import com.randevupazaryeri.user.entity.User;
import com.randevupazaryeri.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private static final DateTimeFormatter NOTIFICATION_TIME_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM EEEE, HH:mm", Locale.forLanguageTag("tr-TR"));


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
        Business business = ownershipService.getBusiness(request.getBusinessId());
        if (business.getStatus() != BusinessStatus.ACTIVE) {
            throw new InvalidAppointmentException("Business is not active");
        }
        List<UUID> serviceIds = resolveServiceIds(request);
        List<ServiceOffer> services = new ArrayList<>();
        for (UUID serviceId : serviceIds) {
            ServiceOffer service = serviceOfferRepository.findByIdAndBusinessId(serviceId, business.getId())
                    .orElseThrow(() -> new InvalidAppointmentException("Service not found in business"));
            if (!service.isActive()) {
                throw new InvalidAppointmentException("Service is inactive");
            }
            services.add(service);
        }
        Employee employee = employeeRepository.findByIdAndBusinessId(request.getEmployeeId(), business.getId())
                .orElseThrow(() -> new InvalidAppointmentException("Employee not found in business"));
        if (!employee.isActive()) {
            throw new InvalidAppointmentException("Employee is inactive");
        }
        if (employee.getUser() != null && employee.getUser().getId().equals(principal.getId())) {
            throw new InvalidAppointmentException("You cannot book an appointment with yourself");
        }
        if (services.stream().anyMatch(service -> !employeeRepository.providesService(employee.getId(), service.getId()))) {
            throw new InvalidAppointmentException("Employee does not provide every selected service");
        }

        Instant start = request.getStartDateTime();
        long totalDurationMinutes = services.stream().mapToLong(ServiceOffer::getDurationMinutes).sum();
        Instant end = start.plus(Duration.ofMinutes(totalDurationMinutes));
        BigDecimal totalPrice = services.stream().map(ServiceOffer::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
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
                .service(services.get(0))
                .services(services)
                .startDateTime(start)
                .endDateTime(end)
                .status(status)
                .price(totalPrice)
                .customerNote(request.getCustomerNote())
                .build();
        try {
            appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException ex) {
            throw new AppointmentConflictException();
        }

        boolean pending = status == AppointmentStatus.PENDING;
        notifyBusinessSide(appointment, "APPOINTMENT_CREATED", pending ? "Yeni randevu talebi" : "Yeni randevu");
        notificationService.notifyUser(principal.getId(), "APPOINTMENT_CREATED",
                pending ? "Randevu talebin gönderildi" : "Randevun oluşturuldu",
                business.getName() + " · " + when(appointment));

        return AppointmentMapper.toResponse(appointment);
    }

    private List<UUID> resolveServiceIds(CreateAppointmentRequest request) {
        List<UUID> requested = request.getServiceIds() == null || request.getServiceIds().isEmpty()
                ? request.getServiceId() == null ? List.of() : List.of(request.getServiceId())
                : request.getServiceIds();
        if (requested.isEmpty() || new LinkedHashSet<>(requested).size() != requested.size()) {
            throw new InvalidAppointmentException("Provide one or more unique services");
        }
        return List.copyOf(requested);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> myAppointments(Pageable pageable) {
        return appointmentRepository.findByCustomerIdOrderByStartDateTimeDesc(SecurityUtils.currentUserId(), pageable)
                .map(AppointmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> businessAppointments(UUID businessId, Pageable pageable) {
        var access = ownershipService.requireMember(businessId);
        Page<Appointment> page = access.isOwner()
                ? appointmentRepository.findByBusinessIdOrderByStartDateTimeDesc(businessId, pageable)
                : appointmentRepository.findByBusinessIdAndEmployeeIdOrderByStartDateTimeDesc(
                        businessId, access.staff().getId(), pageable);
        return page.map(AppointmentMapper::toResponse);
    }

    @Transactional
    public AppointmentResponse cancel(UUID id, CancelAppointmentRequest request) {
        Appointment appointment = getAppointment(id);
        var principal = SecurityUtils.currentPrincipal();
        boolean isCustomer = appointment.getCustomer().getId().equals(principal.getId());
        boolean isOwner = appointment.getBusiness().getOwner().getId().equals(principal.getId());
        boolean isAdmin = principal.getRole() == Role.ADMIN;
        User staffUser = appointment.getEmployee().getUser();
        boolean isStaff = staffUser != null && staffUser.getId().equals(principal.getId());

        if (!isCustomer && !isOwner && !isStaff && !isAdmin) {
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
        boolean wasPending = appointment.getStatus() == AppointmentStatus.PENDING;
        if (wasPending && !isCustomer) {
            requireResponder(appointment);
        }
        String reason = request != null && request.getReason() != null && !request.getReason().isBlank()
                ? request.getReason().trim() : null;
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(reason);

        if (isCustomer) {
            notifyBusinessSide(appointment, "APPOINTMENT_CANCELLED", "Randevu iptal edildi");
        } else {
            String type = wasPending ? "APPOINTMENT_REJECTED" : "APPOINTMENT_CANCELLED";
            String body = appointment.getBusiness().getName() + " · " + when(appointment)
                    + (reason != null ? "\nSebep: " + reason : "");
            notificationService.notifyUser(appointment.getCustomer().getId(), type,
                    wasPending ? "Randevu talebin reddedildi" : "Randevun iptal edildi", body,
                    PushApp.CUSTOMER, pushData(appointment, type));
        }
        return AppointmentMapper.toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse confirm(UUID businessId, UUID id) {
        Appointment appointment = getAccessible(id, businessId);
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new InvalidAppointmentException("Only pending appointments can be confirmed");
        }
        requireResponder(appointment);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        notificationService.notifyUser(appointment.getCustomer().getId(), "APPOINTMENT_CONFIRMED",
                "Randevun onaylandı", appointment.getBusiness().getName() + " · " + when(appointment),
                PushApp.CUSTOMER, pushData(appointment, "APPOINTMENT_CONFIRMED"));
        return AppointmentMapper.toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse complete(UUID businessId, UUID id) {
        Appointment appointment = getAccessible(id, businessId);
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
        Appointment appointment = getAccessible(id, businessId);
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

    /** An expert with their own account handles their appointments; otherwise the owner does. */
    private void notifyBusinessSide(Appointment appointment, String type, String title) {
        String body = String.join(" · ", fullName(appointment.getCustomer()), serviceSummary(appointment), when(appointment));
        User expert = AppointmentMapper.assignedExpert(appointment);
        UUID recipient = expert != null ? expert.getId() : appointment.getBusiness().getOwner().getId();
        notificationService.notifyUser(recipient, type, title, body, PushApp.PARTNER, pushData(appointment, type));
    }

    /** Requests for an expert who has their own account are answered by that expert alone. */
    private void requireResponder(Appointment appointment) {
        var principal = SecurityUtils.currentPrincipal();
        User expert = AppointmentMapper.assignedExpert(appointment);
        if (expert != null && !expert.getId().equals(principal.getId()) && principal.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only the assigned expert can respond to this request");
        }
    }

    /** Owners can act on any appointment of their business; staff only on their own. */
    private Appointment getAccessible(UUID id, UUID businessId) {
        var access = ownershipService.requireMember(businessId);
        Appointment appointment = getInBusiness(id, businessId);
        if (!access.canAccessEmployee(appointment.getEmployee().getId())) {
            throw new ResourceNotFoundException("Appointment not found");
        }
        return appointment;
    }

    private static String fullName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    private static String serviceSummary(Appointment appointment) {
        List<ServiceOffer> services = appointment.getServices();
        if (services == null || services.isEmpty()) {
            return appointment.getService().getName();
        }
        return services.stream().map(ServiceOffer::getName).collect(Collectors.joining(", "));
    }

    private static String when(Appointment appointment) {
        return NOTIFICATION_TIME_FORMAT
                .withZone(ZoneId.of(appointment.getBusiness().getTimezone()))
                .format(appointment.getStartDateTime());
    }

    private static Map<String, String> pushData(Appointment appointment, String type) {
        return Map.of(
                "type", type,
                "appointmentId", appointment.getId().toString(),
                "businessId", appointment.getBusiness().getId().toString());
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
