package com.randevupazaryeri.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.appointment")
public class AppointmentProperties {
    private int customerCancellationDeadlineHours = 2;
}
