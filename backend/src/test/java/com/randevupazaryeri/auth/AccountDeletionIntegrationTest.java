package com.randevupazaryeri.auth;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountDeletionIntegrationTest {

    private static final String[] EMAILS = {
            "delete-owner@test.com", "delete-staff@test.com", "delete-customer@test.com"};

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        PostgresTestSupport.registerDatasource(registry);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean ImageStorageService storage;

    private final List<UUID> userIds = new ArrayList<>();
    String owner;
    String staff;
    String customer;
    String businessId;
    String serviceId;
    UUID employeeId;

    @BeforeEach
    void setUp() throws Exception {
        cleanUp();
        owner = register(EMAILS[0], "PROVIDER", null);
        staff = register(EMAILS[1], "PROVIDER", null);
        customer = register(EMAILS[2], "CUSTOMER", """
                {"termsAccepted":true,"kvkkAcknowledged":true,"privacyAcknowledged":true,
                 "marketingConsent":false,"channel":"IOS_CUSTOMER"}
                """);

        businessId = data(json(post("/api/v1/businesses"), owner, """
                {"name":"Deletion Salon","city":"Istanbul","district":"Kadikoy","timezone":"Europe/Istanbul"}
                """).andExpect(status().isCreated())).path("id").asText();
        serviceId = data(json(post(biz("/services")), owner, """
                {"name":"Cut","durationMinutes":30,"price":100,"currency":"TRY"}
                """).andExpect(status().isCreated())).path("id").asText();
        String code = data(json(post(biz("/invitations")), owner, null).andExpect(status().isCreated()))
                .path("code").asText();
        json(post("/api/v1/invitations/" + code + "/accept"), staff, null).andExpect(status().isOk());
        employeeId = jdbc.queryForObject("SELECT id FROM employees WHERE business_id = ?::uuid", UUID.class, businessId);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void recordsSignUpConsents() {
        List<Map<String, Object>> consents = jdbc.queryForList(
                "SELECT consent_type, granted, channel FROM user_consents WHERE user_id = ? ORDER BY consent_type",
                userId(EMAILS[2]));
        assertThat(consents).extracting(row -> row.get("consent_type"))
                .containsExactly("KVKK", "MARKETING", "PRIVACY", "TERMS");
        assertThat(consents).filteredOn(row -> "MARKETING".equals(row.get("consent_type")))
                .extracting(row -> row.get("granted")).containsExactly(false);
        assertThat(consents).extracting(row -> row.get("channel")).containsOnly("IOS_CUSTOMER");
    }

    @Test
    void recordsNoticeVersionsSeparatelyFromConsents() throws Exception {
        String registration = register("notice-record@test.com", "CUSTOMER", """
                {"termsAccepted":true,"termsVersion":"terms-2.1",
                 "kvkkNoticeVersion":"kvkk-1.3","privacyPolicyVersion":"privacy-1.2",
                 "marketingConsentVersion":"marketing-1.0",
                 "marketingConsent":false,"channel":"WEB_CUSTOMER"}
                """);
        UUID userId = userId("notice-record@test.com");

        assertThat(jdbc.queryForObject(
                "SELECT document_version FROM user_consents WHERE user_id = ? AND consent_type = 'TERMS'",
                String.class, userId)).isEqualTo("terms-2.1");
        assertThat(jdbc.queryForObject(
                "SELECT document_version FROM user_consents WHERE user_id = ? AND consent_type = 'MARKETING'",
                String.class, userId)).isEqualTo("marketing-1.0");

        List<Map<String, Object>> receipts = jdbc.queryForList(
                "SELECT notice_type, document_version, channel, presented_at FROM user_notice_receipts WHERE user_id = ? ORDER BY notice_type",
                userId);
        assertThat(receipts).hasSize(2);
        assertThat(receipts).extracting(row -> row.get("notice_type"))
                .containsExactly("KVKK_NOTICE", "PRIVACY_POLICY");
        assertThat(receipts).extracting(row -> row.get("document_version"))
                .containsExactly("kvkk-1.3", "privacy-1.2");
        assertThat(receipts).extracting(row -> row.get("channel")).containsOnly("WEB_CUSTOMER");
        assertThat(receipts).allSatisfy(row -> assertThat(row.get("presented_at")).isNotNull());
        assertThat(count("SELECT COUNT(*) FROM user_consents WHERE user_id = ? AND consent_type IN ('KVKK', 'PRIVACY')", userId))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM user_consents WHERE user_id = ? AND consent_type = 'TERMS'", userId))
                .isEqualTo(1);
        assertThat(registration).isNotBlank();
    }

    @Test
    void deletesTheCustomerAndKeepsAnonymousHistory() throws Exception {
        UUID customerId = userId(EMAILS[2]);
        UUID past = insertAppointment(customerId, -2, "COMPLETED");
        jdbc.update("INSERT INTO reviews (customer_id, business_id, appointment_id, rating) VALUES (?, ?::uuid, ?, 5)",
                customerId, businessId, past);
        UUID upcoming = insertAppointment(customerId, 2, "CONFIRMED");
        jdbc.update("INSERT INTO favorites (customer_id, business_id) VALUES (?, ?::uuid)", customerId, businessId);
        jdbc.update("""
                INSERT INTO images (storage_provider, public_id, url, folder, owner_type, owner_id)
                VALUES ('cloudinary', ?, 'https://cdn.test/a.webp', 'USER_AVATAR', 'USER', ?)
                """, "resplz/test/" + UUID.randomUUID(), customerId);

        json(post("/api/v1/auth/me/delete"), customer, "{}").andExpect(status().isUnprocessableEntity());
        json(post("/api/v1/auth/me/delete"), customer, "{\"password\":\"wrong-password\"}")
                .andExpect(status().isUnprocessableEntity());
        assertThat(count("SELECT COUNT(*) FROM users WHERE id = ? AND is_active", customerId)).isEqualTo(1);

        doNothing().when(storage).delete(any());
        json(post("/api/v1/auth/me/delete"), customer, "{\"password\":\"Password123!\"}").andExpect(status().isOk());
        verify(storage, times(1)).delete(any());

        Map<String, Object> user = jdbc.queryForMap("SELECT * FROM users WHERE id = ?", customerId);
        assertThat(user.get("first_name")).isEqualTo("Silinmiş");
        assertThat(user.get("is_active")).isEqualTo(false);
        assertThat(user.get("deleted_at")).isNotNull();
        assertThat(user.get("phone")).isNull();
        assertThat((String) user.get("email")).startsWith("deleted-").doesNotContain(EMAILS[2]);

        for (String sql : new String[]{
                "SELECT COUNT(*) FROM user_consents WHERE user_id = ?",
                "SELECT COUNT(*) FROM user_notice_receipts WHERE user_id = ?",
                "SELECT COUNT(*) FROM favorites WHERE customer_id = ?",
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                "SELECT COUNT(*) FROM images WHERE owner_id = ?"}) {
            assertThat(count(sql, customerId)).as(sql).isZero();
        }
        assertThat(jdbc.queryForObject("SELECT status FROM appointments WHERE id = ?", String.class, upcoming))
                .isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT customer_note FROM appointments WHERE id = ?", String.class, past))
                .isNull();
        assertThat(count("SELECT COUNT(*) FROM reviews WHERE appointment_id = ?", past)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'APPOINTMENT_CANCELLED'",
                userId(EMAILS[1]))).isEqualTo(1);

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + customer))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Password123!\"}".formatted(EMAILS[2])))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void keepsTheAccountWhenStorageDeletionFails() throws Exception {
        UUID customerId = userId(EMAILS[2]);
        jdbc.update("""
                INSERT INTO images (storage_provider, public_id, url, folder, owner_type, owner_id)
                VALUES ('cloudinary', ?, 'https://cdn.test/a.webp', 'USER_AVATAR', 'USER', ?)
                """, "resplz/test/" + UUID.randomUUID(), customerId);
        doThrow(new StorageException("down")).when(storage).delete(any());

        json(post("/api/v1/auth/me/delete"), customer, "{\"password\":\"Password123!\"}")
                .andExpect(status().isInternalServerError());
        assertThat(count("SELECT COUNT(*) FROM users WHERE id = ? AND is_active", customerId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM images WHERE owner_id = ?", customerId)).isEqualTo(1);
    }

    @Test
    void ownerHasToDeleteTheBusinessFirst() throws Exception {
        json(post("/api/v1/auth/me/delete"), owner, "{\"password\":\"Password123!\"}")
                .andExpect(status().isUnprocessableEntity());
        assertThat(count("SELECT COUNT(*) FROM users WHERE id = ? AND is_active", userId(EMAILS[0]))).isEqualTo(1);
    }

    @Test
    void deletingAStaffAccountKeepsTheExpertProfile() throws Exception {
        json(post("/api/v1/auth/me/delete"), staff, "{\"password\":\"Password123!\"}").andExpect(status().isOk());
        assertThat(count("SELECT COUNT(*) FROM employees WHERE id = ? AND user_id IS NULL", employeeId)).isEqualTo(1);
    }

    private UUID insertAppointment(UUID customerId, int daysFromNow, String status) {
        UUID id = UUID.randomUUID();
        Instant start = Instant.now().plus(daysFromNow, ChronoUnit.DAYS);
        jdbc.update("""
                INSERT INTO appointments (id, customer_id, business_id, employee_id, service_id, start_date_time,
                    end_date_time, status, price, customer_note)
                VALUES (?, ?, ?::uuid, ?, ?::uuid, ?, ?, ?, 100, 'Kapının şifresi 1234')
                """, id, customerId, businessId, employeeId, serviceId,
                Timestamp.from(start), Timestamp.from(start.plus(30, ChronoUnit.MINUTES)), status);
        jdbc.update("INSERT INTO appointment_services (appointment_id, service_id, service_order) VALUES (?, ?::uuid, 0)",
                id, serviceId);
        return id;
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

    private String register(String email, String role, String consents) throws Exception {
        String body = """
                {"firstName":"T","lastName":"U","email":"%s","password":"Password123!","role":"%s"%s}
                """.formatted(email, role, consents == null ? "" : ",\"consents\":" + consents);
        JsonNode data = data(mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()));
        userIds.add(UUID.fromString(data.path("user").path("id").asText()));
        return data.path("accessToken").asText();
    }

    /** Deleted accounts lose their email, so rows are tracked by id. */
    private void cleanUp() {
        List<String> ids = new ArrayList<>(userIds.stream().map(UUID::toString).toList());
        for (String email : EMAILS) {
            jdbc.queryForList("SELECT id FROM users WHERE email = ?", UUID.class, email)
                    .forEach(id -> ids.add(id.toString()));
        }
        userIds.clear();
        if (ids.isEmpty()) {
            return;
        }
        String users = "'" + String.join("','", ids) + "'";
        String owned = "SELECT id FROM businesses WHERE owner_id IN (" + users + ")";
        jdbc.update("DELETE FROM images WHERE owner_id IN (" + users + ")");
        jdbc.update("DELETE FROM reviews WHERE business_id IN (" + owned + ")");
        jdbc.update("DELETE FROM appointments WHERE business_id IN (" + owned + ")");
        jdbc.update("DELETE FROM businesses WHERE id IN (" + owned + ")");
        jdbc.update("DELETE FROM users WHERE id::text IN (" + users + ")");
    }
}
