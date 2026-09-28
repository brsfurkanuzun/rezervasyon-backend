package com.randevupazaryeri.appointment.mapper;

import com.randevupazaryeri.appointment.dto.AppointmentResponse;
import com.randevupazaryeri.appointment.dto.AppointmentServiceResponse;
import com.randevupazaryeri.appointment.entity.Appointment;

import java.util.List;

public final class AppointmentMapper {
    private AppointmentMapper() {}
    public static AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .customerId(a.getCustomer().getId())
                .businessId(a.getBusiness().getId())
                .businessName(a.getBusiness().getName())
                .employeeId(a.getEmployee().getId())
                .employeeName(a.getEmployee().getFirstName() + " " + a.getEmployee().getLastName())
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
}
