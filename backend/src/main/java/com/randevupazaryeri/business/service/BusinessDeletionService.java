package com.randevupazaryeri.business.service;

import com.randevupazaryeri.appointment.entity.Appointment;
import com.randevupazaryeri.appointment.entity.AppointmentStatus;
import com.randevupazaryeri.appointment.repository.AppointmentRepository;
import com.randevupazaryeri.business.entity.Business;
import com.randevupazaryeri.business.repository.BusinessRepository;
import com.randevupazaryeri.common.security.SecurityUtils;
import com.randevupazaryeri.employee.entity.Employee;
import com.randevupazaryeri.employee.repository.EmployeeRepository;
import com.randevupazaryeri.image.service.ImageService;
import com.randevupazaryeri.notification.service.NotificationService;
import com.randevupazaryeri.push.entity.PushApp;
import com.randevupazaryeri.review.repository.ReviewRepository;
import com.randevupazaryeri.serviceoffer.entity.ServiceOffer;
import com.randevupazaryeri.serviceoffer.repository.ServiceOfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Permanently deletes a business with everything that belongs to it: appointments, reviews, services,
 * expert profiles, hours, invitations, favorites and images. User accounts are kept; linked experts
 * simply lose this workplace. Customers with open appointments are notified.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessDeletionService {

    private static final DateTimeFormatter NOTIFICATION_TIME_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM EEEE, HH:mm", Locale.forLanguageTag("tr-TR"));
    private static final List<AppointmentStatus> OPEN = List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    private final BusinessOwnershipService ownershipService;
    private final BusinessRepository businessRepository;
    private final ServiceOfferRepository serviceOfferRepository;
    private final EmployeeRepository employeeRepository;
    private final AppointmentRepository appointmentRepository;
    private final ReviewRepository reviewRepository;
    private final ImageService imageService;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;

    public void delete(UUID businessId) {
        Business business = ownershipService.requireOwnedBusiness(businessId);
        List<UUID> serviceIds = serviceOfferRepository.findByBusinessId(businessId).stream()
                .map(ServiceOffer::getId).toList();
        List<UUID> employeeIds = employeeRepository.findByBusinessId(businessId).stream()
                .map(Employee::getId).toList();

        // Storage first: if it fails nothing is deleted from the database.
        imageService.deleteAllForBusiness(businessId, serviceIds, employeeIds);

        ZoneId zone = ZoneId.of(business.getTimezone());
        transactionTemplate.executeWithoutResult(status -> {
            List<Appointment> open = appointmentRepository.findOpenByBusinessId(businessId, OPEN, Instant.now());
            for (Appointment appointment : open) {
                notificationService.notifyUser(appointment.getCustomer().getId(), "APPOINTMENT_CANCELLED",
                        "Randevun iptal edildi",
                        business.getName() + " · " + NOTIFICATION_TIME_FORMAT.withZone(zone).format(appointment.getStartDateTime())
                                + "\nİşletme artık ResPlz'de hizmet vermiyor.",
                        PushApp.CUSTOMER, Map.of("type", "APPOINTMENT_CANCELLED"));
            }
            reviewRepository.deleteByBusinessId(businessId);
            appointmentRepository.deleteByBusinessId(businessId);
            businessRepository.deleteById(businessId);
        });
        log.info("Business deleted businessId={} userId={}", businessId, SecurityUtils.currentUserId());
    }
}
