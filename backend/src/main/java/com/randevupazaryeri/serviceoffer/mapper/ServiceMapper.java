package com.randevupazaryeri.serviceoffer.mapper;

import com.randevupazaryeri.serviceoffer.dto.ServiceResponse;
import com.randevupazaryeri.serviceoffer.entity.ServiceOffer;

public final class ServiceMapper {
    private ServiceMapper() {}
    public static ServiceResponse toResponse(ServiceOffer s) {
        return ServiceResponse.builder()
                .id(s.getId()).name(s.getName()).description(s.getDescription())
                .durationMinutes(s.getDurationMinutes()).price(s.getPrice())
                .currency(s.getCurrency()).isActive(s.isActive()).build();
    }
}
