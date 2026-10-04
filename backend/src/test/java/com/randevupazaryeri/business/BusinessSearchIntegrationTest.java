package com.randevupazaryeri.business;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.randevupazaryeri.support.PostgresTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusinessSearchIntegrationTest {

    private static final String OWNER_EMAIL = "search-owner@test.com";
    private static final String CUSTOMER_EMAIL = "search-customer@test.com";
    private static final String ANKARA_BOX = "minLat=39.85&maxLat=39.95&minLng=32.80&maxLng=32.90";

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        PostgresTestSupport.registerDatasource(registry);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    UUID ownerId;
    UUID customerId;

    @BeforeEach
    void setUp() {
        cleanUp();
        ownerId = insertUser(OWNER_EMAIL, "PROVIDER");
        customerId = insertUser(CUSTOMER_EMAIL, "CUSTOMER");

        UUID kizilay = insertBusiness("Kizilay Salon", 39.920, 32.854, "HAIRDRESSER");
        insertService(kizilay, 300);
        insertReview(kizilay, 4);

        UUID tunali = insertBusiness("Tunali Studio", 39.905, 32.860, "BARBER");
        insertService(tunali, 800);
        insertReview(tunali, 5);

        insertBusiness("Cankaya Spa", 39.890, 32.870, "HAIRDRESSER");

        UUID faraway = insertBusiness("Faraway Salon", 41.000, 29.000, "HAIRDRESSER");
        insertService(faraway, 100);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void boundingBoxReturnsOnlyBusinessesInsideTheViewport() throws Exception {
        JsonNode body = search(ANKARA_BOX);

        assertThat(names(body)).containsExactly("Cankaya Spa", "Kizilay Salon", "Tunali Studio");
        assertThat(body.path("pagination").path("totalElements").asLong()).isEqualTo(3);
    }

    @Test
    void distanceSortOrdersNearestFirstAndReportsDistance() throws Exception {
        JsonNode body = search(ANKARA_BOX + "&lat=39.888&lng=32.871&sortBy=distance");

        assertThat(names(body)).containsExactly("Cankaya Spa", "Tunali Studio", "Kizilay Salon");
        assertThat(body.path("data").get(0).path("distanceKm").asDouble()).isLessThan(1.0);
    }

    @Test
    void ratingSortPutsHighestRatedFirstAndUnratedLast() throws Exception {
        assertThat(names(search(ANKARA_BOX + "&sortBy=rating")))
                .containsExactly("Tunali Studio", "Kizilay Salon", "Cankaya Spa");
    }

    @Test
    void priceAndRatingFiltersUseStartingPriceAndAverageRating() throws Exception {
        assertThat(names(search(ANKARA_BOX + "&maxPrice=500"))).containsExactly("Cankaya Spa", "Kizilay Salon");
        assertThat(names(search(ANKARA_BOX + "&minPrice=500"))).containsExactly("Tunali Studio");
        assertThat(names(search(ANKARA_BOX + "&rating=4.5"))).containsExactly("Tunali Studio");
    }

    @Test
    void categoryFilterCombinesWithDistanceSort() throws Exception {
        assertThat(names(search(ANKARA_BOX + "&category=hairdresser&lat=39.888&lng=32.871&sortBy=distance")))
                .containsExactly("Cankaya Spa", "Kizilay Salon");
    }

    private JsonNode search(String query) throws Exception {
        String json = mockMvc.perform(get("/api/v1/businesses?" + query))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private static List<String> names(JsonNode body) {
        List<String> names = new ArrayList<>();
        body.path("data").forEach(b -> names.add(b.path("name").asText()));
        return names;
    }

    private UUID insertUser(String email, String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, first_name, last_name, email, password_hash, role) VALUES (?, 'T', 'U', ?, 'x', ?)",
                id, email, role);
        return id;
    }

    private UUID insertBusiness(String name, double lat, double lng, String categoryCode) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO businesses (id, owner_id, name, slug, city, latitude, longitude, status)
                VALUES (?, ?, ?, ?, 'Ankara', ?, ?, 'ACTIVE')
                """, id, ownerId, name, "search-" + id, lat, lng);
        jdbc.update("INSERT INTO business_categories (business_id, category_id) SELECT ?, id FROM categories WHERE code = ?",
                id, categoryCode);
        return id;
    }

    private UUID insertService(UUID businessId, int price) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO services (id, business_id, name, duration_minutes, price) VALUES (?, ?, 'Kesim', 30, ?)",
                id, businessId, price);
        return id;
    }

    private void insertReview(UUID businessId, int rating) {
        UUID employeeId = UUID.randomUUID();
        jdbc.update("INSERT INTO employees (id, business_id, first_name, last_name) VALUES (?, ?, 'E', 'M')", employeeId, businessId);
        UUID serviceId = jdbc.queryForObject("SELECT id FROM services WHERE business_id = ? LIMIT 1", UUID.class, businessId);
        UUID appointmentId = UUID.randomUUID();
        Instant start = Instant.now().minus(2, ChronoUnit.DAYS);
        jdbc.update("""
                INSERT INTO appointments (id, customer_id, business_id, employee_id, service_id, start_date_time, end_date_time, status, price)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'COMPLETED', 100)
                """, appointmentId, customerId, businessId, employeeId, serviceId,
                Timestamp.from(start), Timestamp.from(start.plus(30, ChronoUnit.MINUTES)));
        jdbc.update("INSERT INTO reviews (customer_id, business_id, appointment_id, rating) VALUES (?, ?, ?, ?)",
                customerId, businessId, appointmentId, rating);
    }

    private void cleanUp() {
        String owned = "SELECT b.id FROM businesses b JOIN users u ON u.id = b.owner_id WHERE u.email = '" + OWNER_EMAIL + "'";
        jdbc.update("DELETE FROM reviews WHERE business_id IN (" + owned + ")");
        jdbc.update("DELETE FROM appointments WHERE business_id IN (" + owned + ")");
        jdbc.update("DELETE FROM business_categories WHERE business_id IN (" + owned + ")");
        jdbc.update("DELETE FROM businesses WHERE id IN (" + owned + ")");
        jdbc.update("DELETE FROM users WHERE email IN (?, ?)", OWNER_EMAIL, CUSTOMER_EMAIL);
    }
}
