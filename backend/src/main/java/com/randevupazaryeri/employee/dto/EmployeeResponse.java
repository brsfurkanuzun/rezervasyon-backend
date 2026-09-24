package com.randevupazaryeri.employee.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class EmployeeResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String title;
    private String bio;
    private String photoUrl;
    private List<String> portfolioUrls;
    private List<String> languages;
    private boolean isActive;
    private List<UUID> serviceIds;
    private Double averageRating;
    private long reviewCount;
}
