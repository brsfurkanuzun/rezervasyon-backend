package com.randevupazaryeri.appointment.mapper;

import com.randevupazaryeri.appointment.dto.AppointmentResponse;
import com.randevupazaryeri.appointment.dto.AppointmentServiceResponse;
import com.randevupazaryeri.appointment.entity.Appointment;
import com.randevupazaryeri.user.entity.User;

import java.util.List;

public final class AppointmentMapper {
    private AppointmentMapper() {}
    public static AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .customerId(a.getCustomer().getId())
                .customerName(a.getCustomer().getFirstName() + " " + a.getCustomer().getLastName())
                .customerPhone(a.getCustomer().getPhone())
                .customerEmail(a.getCustomer().getEmail())
                .customerPhotoUrl(a.getCustomer().getPhotoUrl())
                .businessId(a.getBusiness().getId())
                .businessName(a.getBusiness().getName())
                .businessSlug(a.getBusiness().getSlug())
                .businessCoverImageUrl(a.getBusiness().getCoverImageUrl())
                .businessTimezone(a.getBusiness().getTimezone())
                .employeeId(a.getEmployee().getId())
                .employeeName(a.getEmployee().getFirstName() + " " + a.getEmployee().getLastName())
                .expertManaged(AppointmentMapper.assignedExpert(a) != null)
                .employeePhotoUrl(a.getEmployee().getPhotoUrl())
                .employeeTitle(a.getEmployee().getTitle())
                .serviceId(a.getService().getId())
                .serviceName(a.getService().getName())
                .services(a.getServices() == null || a.getServices().isEmpty()
                    ? List.of(toServiceResponse(a.getService()))
                    : a.getServices().stream().map(AppointmentMapper::toServiceResponse).toList())
                .startDateTime(a.getStartDateTime())
                .endDateTime(a.getEndDateTime())
                .status(a.getStatus())
                .price(a.getPrice())
                .customerNote(a.getCustomerNote())
                .cancellationReason(a.getCancellationReason())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private static AppointmentServiceResponse toServiceResponse(com.randevupazaryeri.serviceoffer.entity.ServiceOffer service) {
        return AppointmentServiceResponse.builder()
                .serviceId(service.getId())
                .serviceName(service.getName())
                .durationMinutes(service.getDurationMinutes())
                .price(service.getPrice())
                .currency(service.getCurrency())
                .build();
    }

    /** The linked staff account of the appointment's expert; null when the owner handles it (unlinked or self). */
    public static User assignedExpert(Appointment a) {
        var user = a.getEmployee().getUser();
        if (user == null || user.getId().equals(a.getBusiness().getOwner().getId())) {
            return null;
        }
        return user;
    }
}
