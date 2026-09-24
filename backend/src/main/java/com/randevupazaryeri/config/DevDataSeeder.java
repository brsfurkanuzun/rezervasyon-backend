package com.randevupazaryeri.config;

import com.randevupazaryeri.appointment.entity.Appointment;
import com.randevupazaryeri.appointment.entity.AppointmentStatus;
import com.randevupazaryeri.appointment.repository.AppointmentRepository;
import com.randevupazaryeri.business.entity.Business;
import com.randevupazaryeri.business.entity.BusinessStatus;
import com.randevupazaryeri.business.entity.Category;
import com.randevupazaryeri.business.repository.BusinessRepository;
import com.randevupazaryeri.business.repository.CategoryRepository;
import com.randevupazaryeri.employee.entity.Employee;
import com.randevupazaryeri.employee.entity.WorkingHour;
import com.randevupazaryeri.employee.repository.EmployeeRepository;
import com.randevupazaryeri.employee.repository.WorkingHourRepository;
import com.randevupazaryeri.review.entity.Review;
import com.randevupazaryeri.review.repository.ReviewRepository;
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
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
    private final AppointmentRepository appointmentRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String PHOTO_AYSE =
            "https://images.pexels.com/photos/774909/pexels-photo-774909.jpeg?auto=compress&cs=tinysrgb&w=600";
    private static final String PHOTO_ZEYNEP =
            "https://images.pexels.com/photos/1239291/pexels-photo-1239291.jpeg?auto=compress&cs=tinysrgb&w=600";
    private static final String PHOTO_ELIF =
            "https://images.pexels.com/photos/1181686/pexels-photo-1181686.jpeg?auto=compress&cs=tinysrgb&w=600";

    private static final List<String> PORTFOLIO_AYSE = List.of(
            "https://images.pexels.com/photos/3993449/pexels-photo-3993449.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/3065209/pexels-photo-3065209.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/3992874/pexels-photo-3992874.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/3992854/pexels-photo-3992854.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/3065171/pexels-photo-3065171.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/3993467/pexels-photo-3993467.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/3993133/pexels-photo-3993133.jpeg?auto=compress&cs=tinysrgb&w=800"
    );
    private static final List<String> PORTFOLIO_ZEYNEP = List.of(
            "https://images.pexels.com/photos/3993320/pexels-photo-3993320.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/3992855/pexels-photo-3992855.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/3738344/pexels-photo-3738344.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/3985329/pexels-photo-3985329.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/3985360/pexels-photo-3985360.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/3997379/pexels-photo-3997379.jpeg?auto=compress&cs=tinysrgb&w=800"
    );
    private static final String LEGACY_PORTFOLIO_URL =
            "https://images.pexels.com/photos/3992870/pexels-photo-3992870.jpeg?auto=compress&cs=tinysrgb&w=800";

    private static final String BIO_AYSE =
            "10 yılı aşkın deneyime sahip stilist. Modern kesimler, balayage ve doğal görünümlü şekillendirme konularında uzmanlaştım. Her misafirin yüz hatlarına ve günlük ritmine uygun, bakımı kolay sonuçlar hedefliyorum.";
    private static final String BIO_ZEYNEP =
            "Renk uygulamaları ve saç sağlığı odaklı çalışan colorist. Soft balayage, kök boyama ve tonlama ile kişiselleştirilmiş renkler oluşturuyorum.";

    private static final String LEGACY_BUSINESS_DESCRIPTION = "Modern beauty salon in Kadikoy";
    private static final String BUSINESS_DESCRIPTION =
            "Studio Nova, Moda'nın kalbinde saç ve güzellik bakımını bir araya getiren butik bir salon. "
                    + "Kesimden renklendirmeye, fön ve özel gün şekillendirmelerine kadar her hizmette kişiye özel "
                    + "danışmanlık sunuyoruz. Ferah ve sakin salonumuzda, saç sağlığını koruyan profesyonel ürünlerle "
                    + "çalışıyor; her misafirimizin kendini iyi hissederek ayrılmasını önemsiyoruz.";
    private static final double BUSINESS_LATITUDE = 40.98135;
    private static final double BUSINESS_LONGITUDE = 29.02573;

    private static final List<String> LANG_AYSE = List.of("Türkçe", "İngilizce");
    private static final List<String> LANG_ZEYNEP = List.of("Türkçe", "İngilizce", "Almanca");

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmailIgnoreCase("admin@randevu.local")) {
            log.info("Seed data already present, backfilling employee profile/media/reviews if needed");
            backfillBusinessProfile();
            backfillEmployeeMediaAndProfile();
            backfillSampleReviews();
            seedNearbyVenues();
            return;
        }
        log.info("Seeding development data...");

        User admin = saveUser("Admin", "User", "admin@randevu.local", Role.ADMIN);
        User provider = saveUser("Nova", "Owner", "provider@randevu.local", Role.PROVIDER);
        User customer = saveUser("Ali", "Musteri", "customer@randevu.local", Role.CUSTOMER);
        User customer2 = saveUser("Elif", "Demir", "elif@randevu.local", Role.CUSTOMER);
        User customer3 = saveUser("Merve", "Koc", "merve@randevu.local", Role.CUSTOMER);

        Category beauty = categoryRepository.findByCode("BEAUTY_SALON").orElseThrow();

        Business business = Business.builder()
                .owner(provider)
                .name("Studio Nova")
                .slug("studio-nova")
                .description(BUSINESS_DESCRIPTION)
                .phone("+905551112233")
                .email("studio@nova.local")
                .address("Caferaga Mah. Moda Cad. No:10")
                .city("Istanbul")
                .district("Kadikoy")
                .latitude(BUSINESS_LATITUDE)
                .longitude(BUSINESS_LONGITUDE)
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
                .bio(BIO_AYSE)
                .photoUrl(PHOTO_AYSE)
                .portfolioUrls(new ArrayList<>(PORTFOLIO_AYSE))
                .languages(new ArrayList<>(LANG_AYSE))
                .isActive(true).services(new HashSet<>(Set.of(haircut, blowdry, color))).build());
        Employee zeynep = employeeRepository.save(Employee.builder()
                .business(business).firstName("Zeynep").lastName("Kaya").title("Colorist")
                .bio(BIO_ZEYNEP)
                .photoUrl(PHOTO_ZEYNEP)
                .portfolioUrls(new ArrayList<>(PORTFOLIO_ZEYNEP))
                .languages(new ArrayList<>(LANG_ZEYNEP))
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

        seedReviewsForEmployee(ayse, business, haircut, List.of(customer, customer2, customer3));
        seedReviewsForEmployee(zeynep, business, color, List.of(customer2, customer3));
        seedNearbyVenues();

        log.info("Seeded users: admin@randevu.local / provider@randevu.local / customer@randevu.local (password: Password123!)");
        log.info("Seeded business: Studio Nova ({})", business.getSlug());
    }

    private record NearbyVenue(String name, String slug, String categoryCode, String description, String address,
                               double latitude, double longitude, String coverImageUrl,
                               String serviceName, int durationMinutes, String price,
                               String employeeFirstName, String employeeLastName, String employeeTitle) {
    }

    private static final List<NearbyVenue> NEARBY_VENUES = List.of(
            new NearbyVenue("Moda Güzellik Atölyesi", "moda-guzellik-atolyesi", "SKIN_CARE",
                    "Cilt bakımı ve yüz uygulamalarında uzman butik stüdyo.", "Moda Cad. No:62",
                    40.98310, 29.02480,
                    "https://images.pexels.com/photos/3985329/pexels-photo-3985329.jpeg?auto=compress&cs=tinysrgb&w=800",
                    "Klasik Cilt Bakımı", 60, "900.00", "Selin", "Aydın", "Cilt Bakım Uzmanı"),
            new NearbyVenue("Bahariye Hair Studio", "bahariye-hair-studio", "HAIRDRESSER",
                    "Kesim, fön ve renklendirmede modern dokunuşlar.", "Bahariye Cad. No:41",
                    40.98760, 29.02870,
                    "https://images.pexels.com/photos/3065171/pexels-photo-3065171.jpeg?auto=compress&cs=tinysrgb&w=800",
                    "Saç Kesimi", 45, "550.00", "Deniz", "Arslan", "Stylist"),
            new NearbyVenue("Kalamış Nail Bar", "kalamis-nail-bar", "NAIL_SALON",
                    "Manikür, pedikür ve nail art için sakin bir mola.", "Kalamış Fener Cad. No:18",
                    40.97820, 29.03720,
                    "https://images.pexels.com/photos/3997379/pexels-photo-3997379.jpeg?auto=compress&cs=tinysrgb&w=800",
                    "Manikür", 40, "400.00", "Ece", "Yıldız", "Nail Artist"),
            new NearbyVenue("Yeldeğirmeni Barber Club", "yeldegirmeni-barber-club", "BARBER",
                    "Klasik berber deneyimi, modern kesimler.", "Karakolhane Cad. No:9",
                    40.99580, 29.02690,
                    "https://images.pexels.com/photos/2061820/pexels-photo-2061820.jpeg?auto=compress&cs=tinysrgb&w=800",
                    "Erkek Saç Kesimi", 30, "350.00", "Emre", "Kılıç", "Berber"),
            new NearbyVenue("Fenerbahçe Spa & Wellness", "fenerbahce-spa-wellness", "SPA",
                    "Masaj ve spa ritüelleriyle şehirden uzaklaşın.", "Fener Kalamış Cad. No:77",
                    40.97130, 29.04380,
                    "https://images.pexels.com/photos/3764568/pexels-photo-3764568.jpeg?auto=compress&cs=tinysrgb&w=800",
                    "Klasik Masaj", 60, "1200.00", "Burak", "Şahin", "Masaj Terapisti")
    );

    private void seedNearbyVenues() {
        User provider = userRepository.findByEmailIgnoreCase("provider@randevu.local").orElse(null);
        if (provider == null) {
            return;
        }
        for (NearbyVenue venue : NEARBY_VENUES) {
            if (businessRepository.findBySlug(venue.slug()).isPresent()) {
                continue;
            }
            Category category = categoryRepository.findByCode(venue.categoryCode())
                    .or(() -> categoryRepository.findByCode("BEAUTY_SALON"))
                    .orElseThrow();
            Business business = businessRepository.save(Business.builder()
                    .owner(provider)
                    .name(venue.name())
                    .slug(venue.slug())
                    .description(venue.description())
                    .address(venue.address())
                    .city("Istanbul")
                    .district("Kadikoy")
                    .latitude(venue.latitude())
                    .longitude(venue.longitude())
                    .coverImageUrl(venue.coverImageUrl())
                    .timezone("Europe/Istanbul")
                    .autoConfirm(true)
                    .status(BusinessStatus.ACTIVE)
                    .categories(new HashSet<>(Set.of(category)))
                    .build());
            ServiceOffer service = serviceOfferRepository.save(ServiceOffer.builder()
                    .business(business).name(venue.serviceName())
                    .durationMinutes(venue.durationMinutes()).price(new BigDecimal(venue.price()))
                    .currency("TRY").isActive(true).build());
            Employee employee = employeeRepository.save(Employee.builder()
                    .business(business).firstName(venue.employeeFirstName()).lastName(venue.employeeLastName())
                    .title(venue.employeeTitle()).isActive(true)
                    .services(new HashSet<>(Set.of(service))).build());
            for (int day = 1; day <= 6; day++) {
                workingHourRepository.save(WorkingHour.builder()
                        .employee(employee).dayOfWeek(day)
                        .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(19, 0))
                        .isAvailable(true).build());
            }
            log.info("Seeded nearby venue: {}", venue.slug());
        }
    }

    private void backfillBusinessProfile() {
        businessRepository.findBySlug("studio-nova").ifPresent(business -> {
            boolean changed = false;
            String description = business.getDescription();
            if (description == null || description.isBlank() || LEGACY_BUSINESS_DESCRIPTION.equals(description)) {
                business.setDescription(BUSINESS_DESCRIPTION);
                changed = true;
            }
            if (business.getLatitude() == null || business.getLongitude() == null) {
                business.setLatitude(BUSINESS_LATITUDE);
                business.setLongitude(BUSINESS_LONGITUDE);
                changed = true;
            }
            if (changed) {
                businessRepository.save(business);
            }
        });
    }

    private static boolean isOutdatedSeedPortfolio(List<String> current, List<String> seed) {
        if (current.isEmpty()) {
            return true;
        }
        Set<String> known = new HashSet<>(PORTFOLIO_AYSE);
        known.addAll(PORTFOLIO_ZEYNEP);
        known.add(LEGACY_PORTFOLIO_URL);
        return known.containsAll(current) && !current.equals(seed);
    }

    private void backfillEmployeeMediaAndProfile() {
        Business studioNova = businessRepository.findBySlug("studio-nova").orElse(null);
        if (studioNova == null) {
            return;
        }
        List<Employee> employees = employeeRepository.findByBusinessId(studioNova.getId()).stream()
                .sorted(java.util.Comparator.comparing(Employee::getFirstName))
                .toList();
        String[] photos = {PHOTO_AYSE, PHOTO_ZEYNEP, PHOTO_ELIF};
        List<List<String>> portfolios = List.of(PORTFOLIO_AYSE, PORTFOLIO_ZEYNEP);
        List<String> bios = List.of(BIO_AYSE, BIO_ZEYNEP);
        List<List<String>> langs = List.of(LANG_AYSE, LANG_ZEYNEP);
        int i = 0;
        for (Employee employee : employees) {
            boolean changed = false;
            if (employee.getPhotoUrl() == null || employee.getPhotoUrl().isBlank()) {
                employee.setPhotoUrl(photos[i % photos.length]);
                changed = true;
            }
            List<String> seedPortfolio = portfolios.get(i % portfolios.size());
            List<String> currentPortfolio = employee.getPortfolioUrls();
            if (currentPortfolio == null || isOutdatedSeedPortfolio(currentPortfolio, seedPortfolio)) {
                employee.setPortfolioUrls(new ArrayList<>(seedPortfolio));
                changed = true;
            }
            if (employee.getBio() == null || employee.getBio().isBlank()) {
                employee.setBio(bios.get(i % bios.size()));
                changed = true;
            }
            if (employee.getLanguages() == null || employee.getLanguages().isEmpty()) {
                employee.setLanguages(new ArrayList<>(langs.get(i % langs.size())));
                changed = true;
            }
            if (changed) {
                employeeRepository.save(employee);
            }
            i++;
        }
    }

    private void backfillSampleReviews() {
        if (reviewRepository.count() > 0) {
            return;
        }
        Business business = businessRepository.findBySlug("studio-nova").orElse(null);
        if (business == null) {
            return;
        }
        List<Employee> employees = employeeRepository.findByBusinessId(business.getId());
        if (employees.isEmpty()) {
            return;
        }
        List<ServiceOffer> services = serviceOfferRepository.findByBusinessId(business.getId());
        if (services.isEmpty()) {
            return;
        }
        User c1 = userRepository.findByEmailIgnoreCase("customer@randevu.local").orElse(null);
        User c2 = userRepository.findByEmailIgnoreCase("elif@randevu.local")
                .orElseGet(() -> saveUser("Elif", "Demir", "elif@randevu.local", Role.CUSTOMER));
        User c3 = userRepository.findByEmailIgnoreCase("merve@randevu.local")
                .orElseGet(() -> saveUser("Merve", "Koc", "merve@randevu.local", Role.CUSTOMER));
        if (c1 == null) {
            return;
        }
        Employee ayse = employees.stream().filter(e -> "Ayse".equals(e.getFirstName())).findFirst().orElse(employees.get(0));
        Employee zeynep = employees.stream().filter(e -> "Zeynep".equals(e.getFirstName())).findFirst().orElse(employees.get(0));
        ServiceOffer serviceA = services.get(0);
        ServiceOffer serviceZ = services.size() > 1 ? services.get(services.size() - 1) : services.get(0);

        seedReviewsForEmployee(ayse, business, serviceA, List.of(c1, c2, c3));
        seedReviewsForEmployee(zeynep, business, serviceZ, List.of(c2, c3));
        log.info("Backfilled sample completed appointments and reviews");
    }

    private void seedReviewsForEmployee(Employee employee, Business business, ServiceOffer service, List<User> customers) {
        String[] comments = {
                "Cok memnun kaldım, sonucu tam istediğim gibi oldu.",
                "Ilgili ve profesyonel. Kesinlikle tekrar geleceğim.",
                "Detaylara dikkat ediyor, ortam da cok rahat."
        };
        int[] ratings = {5, 5, 4};
        for (int i = 0; i < customers.size(); i++) {
            User customer = customers.get(i);
            Instant start = Instant.now().minus(14L + i * 3L, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
            Instant end = start.plus(service.getDurationMinutes(), ChronoUnit.MINUTES);
            Appointment appointment = appointmentRepository.save(Appointment.builder()
                    .customer(customer)
                    .business(business)
                    .employee(employee)
                    .service(service)
                    .startDateTime(start)
                    .endDateTime(end)
                    .status(AppointmentStatus.COMPLETED)
                    .price(service.getPrice())
                    .build());
            reviewRepository.save(Review.builder()
                    .customer(customer)
                    .business(business)
                    .appointment(appointment)
                    .rating(ratings[i % ratings.length])
                    .comment(comments[i % comments.length])
                    .build());
        }
    }

    private User saveUser(String first, String last, String email, Role role) {
        return userRepository.save(User.builder()
                .firstName(first).lastName(last).email(email)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(role).isActive(true).build());
    }
}
