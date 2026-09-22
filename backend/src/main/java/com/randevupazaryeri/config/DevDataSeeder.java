package com.randevupazaryeri.config;

import com.randevupazaryeri.business.entity.Business;
import com.randevupazaryeri.business.entity.BusinessStatus;
import com.randevupazaryeri.business.entity.Category;
import com.randevupazaryeri.business.repository.BusinessRepository;
import com.randevupazaryeri.business.repository.CategoryRepository;
import com.randevupazaryeri.employee.entity.Employee;
import com.randevupazaryeri.employee.entity.WorkingHour;
import com.randevupazaryeri.employee.repository.EmployeeRepository;
import com.randevupazaryeri.employee.repository.WorkingHourRepository;
import com.randevupazaryeri.serviceoffer.entity.ServiceOffer;
import com.randevupazaryeri.serviceoffer.repository.ServiceOfferRepository;
import com.randevupazaryeri.user.entity.Role;
import com.randevupazaryeri.user.entity.User;
import com.randevupazaryeri.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final CategoryRepository categoryRepository;
    private final EmployeeRepository employeeRepository;
    private final ServiceOfferRepository serviceOfferRepository;
    private final WorkingHourRepository workingHourRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmailIgnoreCase("admin@randevu.local")) {
            log.info("Seed data already present, skipping");
            return;
        }
        log.info("Seeding development data...");

        User admin = saveUser("Admin", "User", "admin@randevu.local", Role.ADMIN);
        User provider = saveUser("Nova", "Owner", "provider@randevu.local", Role.PROVIDER);
        User customer = saveUser("Ali", "Musteri", "customer@randevu.local", Role.CUSTOMER);

        Category beauty = categoryRepository.findByCode("BEAUTY_SALON").orElseThrow();

        Business business = Business.builder()
                .owner(provider)
                .name("Studio Nova")
                .slug("studio-nova")
                .description("Modern beauty salon in Kadikoy")
                .phone("+905551112233")
                .email("studio@nova.local")
                .address("Caferaga Mah. Moda Cad. No:10")
                .city("Istanbul")
                .district("Kadikoy")
                .timezone("Europe/Istanbul")
                .autoConfirm(true)
                .status(BusinessStatus.ACTIVE)
                .categories(new HashSet<>(Set.of(beauty)))
                .build();
        businessRepository.save(business);

        ServiceOffer haircut = serviceOfferRepository.save(ServiceOffer.builder()
                .business(business).name("Sac Kesimi").description("Kadin/erkek sac kesimi")
                .durationMinutes(45).price(new BigDecimal("600.00")).currency("TRY").isActive(true).build());
        ServiceOffer blowdry = serviceOfferRepository.save(ServiceOffer.builder()
                .business(business).name("Fon").description("Sac fonu")
                .durationMinutes(30).price(new BigDecimal("300.00")).currency("TRY").isActive(true).build());
        ServiceOffer color = serviceOfferRepository.save(ServiceOffer.builder()
                .business(business).name("Boya").description("Sac boyama")
                .durationMinutes(120).price(new BigDecimal("1500.00")).currency("TRY").isActive(true).build());

        Employee ayse = employeeRepository.save(Employee.builder()
                .business(business).firstName("Ayse").lastName("Yilmaz").title("Stylist")
                .isActive(true).services(new HashSet<>(Set.of(haircut, blowdry, color))).build());
        Employee zeynep = employeeRepository.save(Employee.builder()
                .business(business).firstName("Zeynep").lastName("Kaya").title("Colorist")
                .isActive(true).services(new HashSet<>(Set.of(haircut, color))).build());

        for (Employee e : List.of(ayse, zeynep)) {
            for (int day = 1; day <= 5; day++) {
                workingHourRepository.save(WorkingHour.builder()
                        .employee(e).dayOfWeek(day)
                        .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(13, 0))
                        .isAvailable(true).build());
                workingHourRepository.save(WorkingHour.builder()
                        .employee(e).dayOfWeek(day)
                        .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(18, 0))
                        .isAvailable(true).build());
            }
        }

        log.info("Seeded users: admin@randevu.local / provider@randevu.local / customer@randevu.local (password: Password123!)");
        log.info("Seeded business: Studio Nova ({})", business.getSlug());
    }

    private User saveUser(String first, String last, String email, Role role) {
        return userRepository.save(User.builder()
                .firstName(first).lastName(last).email(email)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(role).isActive(true).build());
    }
}
