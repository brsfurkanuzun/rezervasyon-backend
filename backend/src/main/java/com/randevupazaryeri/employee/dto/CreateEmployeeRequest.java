package com.randevupazaryeri.employee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class CreateEmployeeRequest {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    private String title;
    private String bio;
    private String photoUrl;
    private List<UUID> serviceIds;
}
