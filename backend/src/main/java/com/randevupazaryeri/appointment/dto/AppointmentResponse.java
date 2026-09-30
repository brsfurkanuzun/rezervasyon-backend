package com.randevupazaryeri.appointment.dto;

import com.randevupazaryeri.appointment.entity.AppointmentStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class AppointmentResponse {
    private UUID id;
    private UUID customerId;
    private UUID businessId;
    private String businessName;
    private String businessSlug;
    private String businessCoverImageUrl;
    private String businessTimezone;
    private UUID employeeId;
    private String employeeName;
    private String employeePhotoUrl;
    private String employeeTitle;
    private UUID serviceId;
    private String serviceName;
    private List<AppointmentServiceResponse> services;
    private Instant startDateTime;
    private Instant endDateTime;
    private AppointmentStatus status;
    private BigDecimal price;
    private String customerNote;
    private String cancellationReason;
    private Instant createdAt;
}
