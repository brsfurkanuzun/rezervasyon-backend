package com.randevupazaryeri.employee.mapper;

import com.randevupazaryeri.employee.dto.EmployeeResponse;
import com.randevupazaryeri.employee.entity.Employee;
import com.randevupazaryeri.serviceoffer.entity.ServiceOffer;

import java.util.List;

public final class EmployeeMapper {
    private EmployeeMapper() {}
    public static EmployeeResponse toResponse(Employee e) {
        return EmployeeResponse.builder()
                .id(e.getId()).firstName(e.getFirstName()).lastName(e.getLastName())
                .title(e.getTitle()).bio(e.getBio()).photoUrl(e.getPhotoUrl())
                .portfolioUrls(e.getPortfolioUrls() == null ? List.of() : List.copyOf(e.getPortfolioUrls()))
                .languages(e.getLanguages() == null ? List.of() : List.copyOf(e.getLanguages()))
                .isActive(e.isActive())
                .serviceIds(e.getServices().stream().map(ServiceOffer::getId).toList())
                .build();
    }
}
