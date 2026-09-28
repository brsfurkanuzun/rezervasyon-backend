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
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
            seedAdditionalSampleReviewers();
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
                .coverImageUrl("https://images.pexels.com/photos/3993449/pexels-photo-3993449.jpeg?auto=compress&cs=tinysrgb&w=1200")
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
        seedAdditionalSampleReviewers();

        log.info("Seeded users: admin@randevu.local / provider@randevu.local / customer@randevu.local (password: Password123!)");
        log.info("Seeded business: Studio Nova ({})", business.getSlug());
    }

    record SeedService(String name, int durationMinutes, String price) {
    }

    record SeedEmployee(String firstName, String lastName, String title, String photoUrl,
                        List<String> serviceNames) {
    }

    record NearbyVenue(String name, String slug, String categoryCode, String description, String address,
                       double latitude, double longitude, String coverImageUrl,
                       List<SeedService> services, List<SeedEmployee> employees) {
    }

    record SampleReviewer(String firstName, String lastName, String email) {
    }

    static final List<SampleReviewer> ADDITIONAL_REVIEWERS = List.of(
            new SampleReviewer("Derya", "Acar", "demo.customer01@randevu.local"),
            new SampleReviewer("Onur", "Eren", "demo.customer02@randevu.local"),
            new SampleReviewer("Seda", "Yilmaz", "demo.customer03@randevu.local"),
            new SampleReviewer("Can", "Ozkan", "demo.customer04@randevu.local"),
            new SampleReviewer("Irem", "Aras", "demo.customer05@randevu.local"),
            new SampleReviewer("Burak", "Deniz", "demo.customer06@randevu.local"),
            new SampleReviewer("Ece", "Aksoy", "demo.customer07@randevu.local")
    );

        static final List<NearbyVenue> NEARBY_VENUES = List.of(
            new NearbyVenue("Moda Güzellik Atölyesi", "moda-guzellik-atolyesi", "SKIN_CARE",
                "Cilt bakımı ve yüz uygulamalarında uzman butik stüdyo.", "Moda Cad. No:62",
                40.98310, 29.02480,
                "https://images.pexels.com/photos/3985329/pexels-photo-3985329.jpeg?auto=compress&cs=tinysrgb&w=800",
                List.of(new SeedService("Klasik Cilt Bakımı", 60, "900.00"),
                    new SeedService("Hydra Nem Terapisi", 45, "750.00"),
                    new SeedService("Arındırıcı Peeling", 50, "800.00")),
                List.of(new SeedEmployee("Selin", "Aydın", "Cilt Bakım Uzmanı", PHOTO_AYSE,
                        List.of("Klasik Cilt Bakımı", "Hydra Nem Terapisi", "Arındırıcı Peeling")),
                    new SeedEmployee("Aylin", "Demir", "Estetisyen", PHOTO_ZEYNEP,
                        List.of("Klasik Cilt Bakımı", "Hydra Nem Terapisi")),
                    new SeedEmployee("Yasemin", "Can", "Cilt Terapisti", PHOTO_ELIF,
                        List.of("Hydra Nem Terapisi", "Arındırıcı Peeling")))),
            new NearbyVenue("Bahariye Hair Studio", "bahariye-hair-studio", "HAIRDRESSER",
                "Kesim, fön ve renklendirmede modern dokunuşlar.", "Bahariye Cad. No:41",
                40.98760, 29.02870,
                "https://images.pexels.com/photos/3065171/pexels-photo-3065171.jpeg?auto=compress&cs=tinysrgb&w=800",
                List.of(new SeedService("Saç Kesimi", 45, "550.00"),
                    new SeedService("Fön", 30, "300.00"),
                    new SeedService("Boya", 120, "1500.00"),
                    new SeedService("Balayage", 180, "2800.00")),
                List.of(new SeedEmployee("Deniz", "Arslan", "Stylist", PHOTO_AYSE,
                        List.of("Saç Kesimi", "Fön", "Boya", "Balayage")),
                    new SeedEmployee("Mert", "Kaya", "Kesim Uzmanı", PHOTO_ZEYNEP,
                        List.of("Saç Kesimi", "Fön")),
                    new SeedEmployee("Ece", "Şahin", "Colorist", PHOTO_ELIF,
                        List.of("Boya", "Balayage")),
                    new SeedEmployee("Dilan", "Aslan", "Stylist", PHOTO_AYSE,
                        List.of("Saç Kesimi", "Balayage")))),
            new NearbyVenue("Kalamış Nail Bar", "kalamis-nail-bar", "NAIL_SALON",
                "Manikür, pedikür ve nail art için sakin bir mola.", "Kalamış Fener Cad. No:18",
                40.97820, 29.03720,
                "https://images.pexels.com/photos/3997379/pexels-photo-3997379.jpeg?auto=compress&cs=tinysrgb&w=800",
                List.of(new SeedService("Manikür", 40, "400.00"),
                    new SeedService("Pedikür", 50, "550.00"),
                    new SeedService("Nail Art", 60, "700.00")),
                List.of(new SeedEmployee("Ece", "Yıldız", "Nail Artist", PHOTO_ZEYNEP,
                        List.of("Manikür", "Pedikür", "Nail Art")),
                    new SeedEmployee("Duru", "Akın", "Nail Artist", PHOTO_ELIF,
                        List.of("Manikür", "Nail Art")))),
            new NearbyVenue("Yeldeğirmeni Barber Club", "yeldegirmeni-barber-club", "BARBER",
                "Klasik berber deneyimi, modern kesimler.", "Karakolhane Cad. No:9",
                40.99580, 29.02690,
                "https://images.pexels.com/photos/2061820/pexels-photo-2061820.jpeg?auto=compress&cs=tinysrgb&w=800",
                List.of(new SeedService("Erkek Saç Kesimi", 30, "350.00"),
                    new SeedService("Sakal Tasarımı", 25, "250.00"),
                    new SeedService("Sıcak Havlu Bakımı", 20, "200.00")),
                List.of(new SeedEmployee("Emre", "Kılıç", "Berber", PHOTO_AYSE,
                        List.of("Erkek Saç Kesimi", "Sakal Tasarımı", "Sıcak Havlu Bakımı")),
                    new SeedEmployee("Ali", "Yaman", "Berber", PHOTO_ZEYNEP,
                        List.of("Erkek Saç Kesimi", "Sakal Tasarımı")),
                    new SeedEmployee("Kaan", "Polat", "Berber", PHOTO_ELIF,
                        List.of("Sakal Tasarımı", "Sıcak Havlu Bakımı")))),
            new NearbyVenue("Fenerbahçe Spa & Wellness", "fenerbahce-spa-wellness", "SPA",
                "Masaj ve spa ritüelleriyle şehirden uzaklaşın.", "Fener Kalamış Cad. No:77",
                40.97130, 29.04380,
                "https://images.pexels.com/photos/3764568/pexels-photo-3764568.jpeg?auto=compress&cs=tinysrgb&w=800",
                List.of(new SeedService("Klasik Masaj", 60, "1200.00"),
                    new SeedService("Aromaterapi Masajı", 60, "1400.00"),
                    new SeedService("Sıcak Taş Terapisi", 75, "1700.00"),
                    new SeedService("Çift Masajı", 90, "2400.00")),
                List.of(new SeedEmployee("Burak", "Şahin", "Masaj Terapisti", PHOTO_AYSE,
                        List.of("Klasik Masaj", "Aromaterapi Masajı")),
                    new SeedEmployee("Mina", "Er", "Spa Terapisti", PHOTO_ZEYNEP,
                        List.of("Klasik Masaj", "Sıcak Taş Terapisi")),
                    new SeedEmployee("Can", "Özer", "Masaj Terapisti", PHOTO_ELIF,
                        List.of("Aromaterapi Masajı", "Çift Masajı")),
                    new SeedEmployee("Eylül", "Tan", "Spa Terapisti", PHOTO_AYSE,
                        List.of("Sıcak Taş Terapisi", "Çift Masajı")))),
            new NearbyVenue("Koşuyolu Cilt Stüdyosu", "kosuyolu-cilt-studyosu", "SKIN_CARE",
                "Kişiye özel cilt bakımı ve nem terapileri.", "Koşuyolu Mah. Kalfaçeşme Sok. No:11",
                41.00010, 29.05510,
                "https://images.pexels.com/photos/3993320/pexels-photo-3993320.jpeg?auto=compress&cs=tinysrgb&w=800",
                List.of(new SeedService("Hydra Cilt Bakımı", 60, "850.00"),
                    new SeedService("Leke Karşıtı Bakım", 70, "1100.00"),
                    new SeedService("Yüz Masajı", 35, "500.00")),
                List.of(new SeedEmployee("Selin", "Aydin", "Cilt Bakım Uzmanı", PHOTO_ZEYNEP,
                        List.of("Hydra Cilt Bakımı", "Leke Karşıtı Bakım")),
                    new SeedEmployee("Nehir", "Ekin", "Estetisyen", PHOTO_ELIF,
                        List.of("Hydra Cilt Bakımı", "Leke Karşıtı Bakım", "Yüz Masajı")))),
            new NearbyVenue("Suadiye Nail Atelier", "suadiye-nail-atelier", "NAIL_SALON",
                "Manikür, kalıcı oje ve sade nail art uygulamaları.", "Suadiye Mah. Bağdat Cad. No:402",
                40.96370, 29.07820,
                "https://images.pexels.com/photos/3992855/pexels-photo-3992855.jpeg?auto=compress&cs=tinysrgb&w=800",
                List.of(new SeedService("Kalıcı Oje", 50, "650.00"),
                    new SeedService("Jel Güçlendirme", 75, "900.00"),
                    new SeedService("Manikür", 40, "450.00"),
                    new SeedService("Nail Art Tasarımı", 30, "350.00")),
                List.of(new SeedEmployee("Ceren", "Aksoy", "Nail Artist", PHOTO_AYSE,
                        List.of("Kalıcı Oje", "Jel Güçlendirme", "Nail Art Tasarımı")),
                    new SeedEmployee("İdil", "Sönmez", "Nail Artist", PHOTO_ZEYNEP,
                        List.of("Kalıcı Oje", "Manikür")),
                    new SeedEmployee("Lara", "Ateş", "Nail Artist", PHOTO_ELIF,
                        List.of("Jel Güçlendirme", "Manikür", "Nail Art Tasarımı")))),
            new NearbyVenue("Caddebostan Brow & Lash", "caddebostan-brow-lash", "BEAUTY_SALON",
                "Kaş tasarımı ve kirpik uygulamalarında doğal sonuçlar.", "Caddebostan Mah. Plaj Yolu Sok. No:6",
                40.96780, 29.06390,
                "https://images.pexels.com/photos/3985360/pexels-photo-3985360.jpeg?auto=compress&cs=tinysrgb&w=800",
                List.of(new SeedService("Kaş Tasarımı", 40, "500.00"),
                    new SeedService("Kirpik Lifting", 60, "850.00"),
                    new SeedService("Kaş Laminasyonu", 55, "750.00")),
                List.of(new SeedEmployee("Buse", "Demir", "Brow Artist", PHOTO_ZEYNEP,
                        List.of("Kaş Tasarımı", "Kaş Laminasyonu")),
                    new SeedEmployee("Ada", "Güneş", "Lash Artist", PHOTO_ELIF,
                        List.of("Kirpik Lifting", "Kaş Laminasyonu")))),
            new NearbyVenue("Acıbadem Masaj Stüdyosu", "acibadem-masaj-studyosu", "SPA",
                "Aromaterapi ve rahatlatıcı masaj seansları.", "Acıbadem Cad. No:114",
                41.00090, 29.04270,
                "https://images.pexels.com/photos/3738344/pexels-photo-3738344.jpeg?auto=compress&cs=tinysrgb&w=800",
                List.of(new SeedService("Aromaterapi Masajı", 60, "1400.00"),
                    new SeedService("Derin Doku Masajı", 60, "1500.00"),
                    new SeedService("Refleksoloji", 45, "1100.00")),
                List.of(new SeedEmployee("Kerem", "Aslan", "Masaj Terapisti", PHOTO_AYSE,
                        List.of("Aromaterapi Masajı", "Derin Doku Masajı")),
                    new SeedEmployee("Deniz", "Uçar", "Refleksoloji Uzmanı", PHOTO_ZEYNEP,
                        List.of("Refleksoloji", "Aromaterapi Masajı")),
                    new SeedEmployee("Eren", "Gül", "Masaj Terapisti", PHOTO_ELIF,
                        List.of("Derin Doku Masajı", "Refleksoloji"))))
        );

    void seedNearbyVenues() {
        User provider = userRepository.findByEmailIgnoreCase("provider@randevu.local").orElse(null);
        if (provider == null) {
            return;
        }
        for (NearbyVenue venue : NEARBY_VENUES) {
            Category category = categoryRepository.findByCode(venue.categoryCode())
                    .or(() -> categoryRepository.findByCode("BEAUTY_SALON"))
                    .orElseThrow();
            Business business = businessRepository.findBySlug(venue.slug()).orElse(null);
            if (business == null) {
                business = businessRepository.save(Business.builder()
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
            } else {
                boolean changed = false;
                if (business.getCoverImageUrl() == null || business.getCoverImageUrl().isBlank()) {
                    business.setCoverImageUrl(venue.coverImageUrl());
                    changed = true;
                }
                Set<Category> categories = business.getCategories() == null
                        ? new HashSet<>() : new HashSet<>(business.getCategories());
                if (categories.add(category)) {
                    business.setCategories(categories);
                    changed = true;
                }
                if (changed) businessRepository.save(business);
            }

            Map<String, ServiceOffer> servicesByName = new HashMap<>();
            serviceOfferRepository.findByBusinessId(business.getId())
                    .forEach(service -> servicesByName.put(service.getName(), service));
            for (SeedService seedService : venue.services()) {
                ServiceOffer service = servicesByName.get(seedService.name());
                if (service == null) {
                    service = serviceOfferRepository.save(ServiceOffer.builder()
                            .business(business)
                            .name(seedService.name())
                            .durationMinutes(seedService.durationMinutes())
                            .price(new BigDecimal(seedService.price()))
                            .currency("TRY")
                            .isActive(true)
                            .build());
                    servicesByName.put(seedService.name(), service);
                } else if (!service.isActive()) {
                    service.setActive(true);
                    serviceOfferRepository.save(service);
                }
            }

                List<Employee> existingEmployees = employeeRepository.findByBusinessId(business.getId());
                Set<Employee> matchedEmployees = new HashSet<>();
                Set<String> configuredNames = venue.employees().stream()
                    .map(employee -> normalizeEmployeeName(employee.firstName() + " " + employee.lastName()))
                    .collect(java.util.stream.Collectors.toSet());
            for (SeedEmployee seedEmployee : venue.employees()) {
                String fullName = seedEmployee.firstName() + " " + seedEmployee.lastName();
                Employee employee = existingEmployees.stream()
                    .filter(candidate -> !matchedEmployees.contains(candidate))
                    .filter(candidate -> (candidate.getFirstName() + " " + candidate.getLastName()).equals(fullName))
                    .findFirst()
                    .orElseGet(() -> existingEmployees.stream()
                        .filter(candidate -> !matchedEmployees.contains(candidate))
                        .filter(candidate -> normalizeEmployeeName(candidate.getFirstName() + " " + candidate.getLastName())
                            .equals(normalizeEmployeeName(fullName)))
                        .findFirst()
                        .orElse(null));
                if (employee == null) {
                    employee = Employee.builder()
                            .business(business)
                            .firstName(seedEmployee.firstName())
                            .lastName(seedEmployee.lastName())
                            .build();
                } else {
                    matchedEmployees.add(employee);
                }

                Set<ServiceOffer> assignedServices = employee.getServices() == null
                        ? new HashSet<>() : new HashSet<>(employee.getServices());
                for (String serviceName : seedEmployee.serviceNames()) {
                    ServiceOffer service = servicesByName.get(serviceName);
                    if (service == null) {
                        throw new IllegalStateException("Unknown seeded service " + serviceName + " for " + venue.slug());
                    }
                    assignedServices.add(service);
                }
                employee.setServices(assignedServices);
                employee.setActive(true);
                if (employee.getTitle() == null || employee.getTitle().isBlank()) {
                    employee.setTitle(seedEmployee.title());
                }
                if (employee.getPhotoUrl() == null || employee.getPhotoUrl().isBlank()) {
                    employee.setPhotoUrl(seedEmployee.photoUrl());
                }
                employee = employeeRepository.save(employee);
                matchedEmployees.add(employee);

                if (workingHourRepository.findByEmployeeId(employee.getId()).isEmpty()) {
                    for (int day = 1; day <= 6; day++) {
                        workingHourRepository.save(WorkingHour.builder()
                                .employee(employee).dayOfWeek(day)
                                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(19, 0))
                                .isAvailable(true).build());
                    }
                }
            }
            for (Employee employee : existingEmployees) {
                String existingName = employee.getFirstName() + " " + employee.getLastName();
                if (!matchedEmployees.contains(employee) && configuredNames.contains(normalizeEmployeeName(existingName))
                        && employee.isActive()) {
                    employee.setActive(false);
                    employeeRepository.save(employee);
                }
            }
            log.info("Seeded nearby venue catalog and team: {}", venue.slug());
        }
    }

    static String normalizeEmployeeName(String name) {
        String turkishI = name.replace('ı', 'i').replace('İ', 'I');
        return Normalizer.normalize(turkishI, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }

    private void seedAdditionalSampleReviewers() {
        List<User> existingReviewers = List.of(
                ensureCustomer("Ali", "Musteri", "customer@randevu.local"),
                ensureCustomer("Elif", "Demir", "elif@randevu.local"),
                ensureCustomer("Merve", "Koc", "merve@randevu.local")
        );
        List<User> additionalReviewers = ADDITIONAL_REVIEWERS.stream()
                .map(reviewer -> ensureCustomer(reviewer.firstName(), reviewer.lastName(), reviewer.email()))
                .toList();

        for (int i = 0; i < NEARBY_VENUES.size(); i++) {
            NearbyVenue venue = NEARBY_VENUES.get(i);
            Business business = businessRepository.findBySlug(venue.slug()).orElse(null);
            if (business == null) continue;

            User reviewer = i < additionalReviewers.size()
                    ? additionalReviewers.get(i)
                    : existingReviewers.get(i - additionalReviewers.size());
            if (reviewRepository.existsByCustomerIdAndBusinessId(reviewer.getId(), business.getId())) continue;

            List<Employee> employees = employeeRepository.findByBusinessId(business.getId());
            List<ServiceOffer> services = serviceOfferRepository.findByBusinessId(business.getId());
            if (!employees.isEmpty() && !services.isEmpty()) {
                seedReviewsForEmployee(employees.get(0), business, services.get(0), List.of(reviewer));
            }
        }
    }

    private User ensureCustomer(String firstName, String lastName, String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> saveUser(firstName, lastName, email, Role.CUSTOMER));
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
            if (business.getCoverImageUrl() == null || business.getCoverImageUrl().isBlank()) {
                business.setCoverImageUrl("https://images.pexels.com/photos/3993449/pexels-photo-3993449.jpeg?auto=compress&cs=tinysrgb&w=1200");
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
                    .services(List.of(service))
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
