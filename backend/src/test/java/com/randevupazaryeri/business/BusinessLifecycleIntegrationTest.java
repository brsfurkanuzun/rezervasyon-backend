package com.randevupazaryeri.business;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.randevupazaryeri.image.storage.ImageStorageService;
import com.randevupazaryeri.image.storage.StorageException;
import com.randevupazaryeri.support.PostgresTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusinessLifecycleIntegrationTest {

    private static final String[] EMAILS = {
            "life-owner@test.com", "life-staff@test.com", "life-customer@test.com", "life-other@test.com"};

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        PostgresTestSupport.registerDatasource(registry);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean ImageStorageService storage;

    String owner;
    String staff;
    String customer;
    String other;
    String businessId;
    String slug;
    String serviceId;

    @BeforeEach
    void setUp() throws Exception {
        cleanUp();
        owner = registerAndLogin(EMAILS[0], "PROVIDER");
        staff = registerAndLogin(EMAILS[1], "PROVIDER");
        customer = registerAndLogin(EMAILS[2], "CUSTOMER");
        other = registerAndLogin(EMAILS[3], "PROVIDER");

        JsonNode business = data(json(post("/api/v1/businesses"), owner, """
                {"name":"Lifecycle Salon","city":"Istanbul","district":"Kadikoy","timezone":"Europe/Istanbul"}
                """).andExpect(status().isCreated()));
        businessId = business.path("id").asText();
        slug = business.path("slug").asText();
        serviceId = data(json(post(biz("/services")), owner, """
                {"name":"Cut","durationMinutes":30,"price":100,"currency":"TRY"}
                """).andExpect(status().isCreated())).path("id").asText();
        String code = data(json(post(biz("/invitations")), owner, null).andExpect(status().isCreated()))
                .path("code").asText();
        json(post("/api/v1/invitations/" + code + "/accept"), staff, null).andExpect(status().isOk());
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void ownerFreezesAndReactivatesTheBusiness() throws Exception {
        json(post(biz("/freeze")), other, null).andExpect(status().isForbidden());
        json(post(biz("/freeze")), staff, null).andExpect(status().isForbidden());

        json(post(biz("/freeze")), owner, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
        mockMvc.perform(get("/api/v1/businesses/" + slug)).andExpect(status().isNotFound());
        json(post(biz("/freeze")), owner, null).andExpect(status().isUnprocessableEntity());

        json(post(biz("/unfreeze")), owner, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        mockMvc.perform(get("/api/v1/businesses/" + slug)).andExpect(status().isOk());
        json(post(biz("/unfreeze")), owner, null).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void ownerCannotReactivateASuspendedBusiness() throws Exception {
        jdbc.update("UPDATE businesses SET status = 'SUSPENDED' WHERE id = ?::uuid", businessId);
        json(post(biz("/unfreeze")), owner, null).andExpect(status().isUnprocessableEntity());
        assertThat(businessStatus()).isEqualTo("SUSPENDED");
    }

    @Test
    void deletingRemovesEveryRecordAndKeepsUserAccounts() throws Exception {
        UUID employeeId = jdbc.queryForObject("SELECT id FROM employees WHERE business_id = ?::uuid", UUID.class, businessId);
        UUID customerId = userId(EMAILS[2]);
        UUID past = insertAppointment(customerId, employeeId, -2, "COMPLETED");
        jdbc.update("INSERT INTO reviews (customer_id, business_id, appointment_id, rating) VALUES (?, ?::uuid, ?, 5)",
                customerId, businessId, past);
        insertAppointment(customerId, employeeId, 2, "CONFIRMED");
        jdbc.update("INSERT INTO favorites (customer_id, business_id) VALUES (?, ?::uuid)", customerId, businessId);
        insertImage("BUSINESS", "BUSINESS_COVER", businessId);
        insertImage("SERVICE", "SERVICE_IMAGE", serviceId);
        insertImage("EMPLOYEE", "EMPLOYEE_PHOTO", employeeId.toString());

        json(delete(biz("")), staff, null).andExpect(status().isForbidden());
        json(delete(biz("")), other, null).andExpect(status().isForbidden());

        doThrow(new StorageException("down")).when(storage).delete(any());
        json(delete(biz("")), owner, null).andExpect(status().isInternalServerError());
        assertThat(count("SELECT COUNT(*) FROM businesses WHERE id = ?::uuid", businessId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM appointments WHERE business_id = ?::uuid", businessId)).isEqualTo(2);

        doNothing().when(storage).delete(any());
        json(delete(biz("")), owner, null).andExpect(status().isNoContent());
        verify(storage, times(4)).delete(any());

        assertThat(count("SELECT COUNT(*) FROM businesses WHERE id = ?::uuid", businessId)).isZero();
        for (String table : new String[]{"appointments", "reviews", "favorites", "services", "employees", "employee_invitations"}) {
            assertThat(count("SELECT COUNT(*) FROM " + table + " WHERE business_id = ?::uuid", businessId))
                    .as(table).isZero();
        }
        assertThat(count("SELECT COUNT(*) FROM images WHERE owner_id IN (?::uuid, ?::uuid, ?)", businessId, serviceId, employeeId))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM users WHERE email IN (?, ?, ?)", EMAILS[0], EMAILS[1], EMAILS[2])).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'APPOINTMENT_CANCELLED'", customerId))
                .isEqualTo(1);

        mockMvc.perform(get("/api/v1/provider/workplaces").header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(get("/api/v1/businesses/" + slug)).andExpect(status().isNotFound());
    }

    private UUID insertAppointment(UUID customerId, UUID employeeId, int daysFromNow, String status) {
        UUID id = UUID.randomUUID();
        Instant start = Instant.now().plus(daysFromNow, ChronoUnit.DAYS);
        jdbc.update("""
                INSERT INTO appointments (id, customer_id, business_id, employee_id, service_id, start_date_time, end_date_time, status, price)
                VALUES (?, ?, ?::uuid, ?, ?::uuid, ?, ?, ?, 100)
                """, id, customerId, businessId, employeeId, serviceId,
                Timestamp.from(start), Timestamp.from(start.plus(30, ChronoUnit.MINUTES)), status);
        jdbc.update("INSERT INTO appointment_services (appointment_id, service_id, service_order) VALUES (?, ?::uuid, 0)",
                id, serviceId);
        return id;
    }

    private void insertImage(String ownerType, String folder, String ownerId) {
        jdbc.update("""
                INSERT INTO images (storage_provider, public_id, url, folder, owner_type, owner_id)
                VALUES ('cloudinary', ?, 'https://cdn.test/x.webp', ?, ?, ?::uuid)
                """, "resplz/test/" + UUID.randomUUID(), folder, ownerType, ownerId);
    }

    private String businessStatus() {
        return jdbc.queryForObject("SELECT status FROM businesses WHERE id = ?::uuid", String.class, businessId);
    }

    private UUID userId(String email) {
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", UUID.class, email);
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private ResultActions json(MockHttpServletRequestBuilder request, String token, String body) throws Exception {
        request.header("Authorization", "Bearer " + token);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }

    private JsonNode data(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).path("data");
    }

    private String biz(String path) {
        return "/api/v1/businesses/" + businessId + path;
    }

    private String registerAndLogin(String email, String role) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"T","lastName":"U","email":"%s","password":"Password123!","role":"%s"}
                """.formatted(email, role))).andExpect(status().isCreated());
        return objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Password123!\"}".formatted(email)))
                .andReturn().getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }

    /** Only this test's rows: the shared local database also holds development data. */
    private void cleanUp() {
        String users = "SELECT id FROM users WHERE email IN ('" + String.join("','", EMAILS) + "')";
        String owned = "SELECT id FROM businesses WHERE owner_id IN (" + users + ")";
        jdbc.update("DELETE FROM images WHERE owner_id IN (" + owned + ")"
                + " OR owner_id IN (SELECT id FROM services WHERE business_id IN (" + owned + "))"
                + " OR owner_id IN (SELECT id FROM employees WHERE business_id IN (" + owned + "))");
        jdbc.update("DELETE FROM reviews WHERE business_id IN (" + owned + ")");
        jdbc.update("DELETE FROM appointments WHERE business_id IN (" + owned + ")");
        jdbc.update("DELETE FROM businesses WHERE id IN (" + owned + ")");
        jdbc.update("DELETE FROM refresh_tokens WHERE user_id IN (" + users + ")");
        jdbc.update("DELETE FROM users WHERE id IN (" + users + ")");
    }
}
