package com.randevupazaryeri.appointment.mapper;

import com.randevupazaryeri.appointment.dto.AppointmentResponse;
import com.randevupazaryeri.appointment.entity.Appointment;

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
                .startDateTime(a.getStartDateTime())
                .endDateTime(a.getEndDateTime())
                .status(a.getStatus())
                .price(a.getPrice())
                .customerNote(a.getCustomerNote())
                .cancellationReason(a.getCancellationReason())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
