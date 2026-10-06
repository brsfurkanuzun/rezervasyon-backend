package com.randevupazaryeri.appointment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppointmentLookupIntegrationTest {

    private static final String[] EMAILS = {"lookup-owner@test.com", "lookup-customer@test.com", "lookup-other@test.com"};

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        PostgresTestSupport.registerDatasource(registry);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    String owner;
    String customer;
    String other;
    String businessId;
    String serviceId;
    UUID employeeId;

    @BeforeEach
    void setUp() throws Exception {
        cleanUp();
        owner = register(EMAILS[0], "PROVIDER");
        customer = register(EMAILS[1], "CUSTOMER");
        other = register(EMAILS[2], "CUSTOMER");
        businessId = data(json(post("/api/v1/businesses"), owner, """
                {"name":"Lookup Salon","city":"Istanbul","district":"Kadikoy","timezone":"Europe/Istanbul"}
                """).andExpect(status().isCreated())).path("id").asText();
        serviceId = data(json(post(biz("/services")), owner, """
                {"name":"Cut","durationMinutes":30,"price":100,"currency":"TRY"}
                """).andExpect(status().isCreated())).path("id").asText();
        data(json(post(biz("/employees/self")), owner, null).andExpect(status().is2xxSuccessful()));
        employeeId = jdbc.queryForObject("SELECT id FROM employees WHERE business_id = ?::uuid", UUID.class, businessId);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void customerAndOwnerCanOpenAnAppointmentOthersCannot() throws Exception {
        UUID id = insertAppointment(-1);
        assertThat(data(json(get("/api/v1/appointments/" + id), customer, null).andExpect(status().isOk()))
                .path("id").asText()).isEqualTo(id.toString());
        json(get("/api/v1/appointments/" + id), owner, null).andExpect(status().isOk());
        json(get("/api/v1/appointments/" + id), other, null).andExpect(status().isForbidden());
        json(get("/api/v1/appointments/" + UUID.randomUUID()), customer, null).andExpect(status().isNotFound());
    }

    @Test
    void filtersBusinessAppointmentsByStartTime() throws Exception {
        UUID old = insertAppointment(-120);
        UUID recent = insertAppointment(-3);
        UUID upcoming = insertAppointment(5);
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        assertThat(ids(biz("/appointments?size=50"))).containsExactly(upcoming, recent, old);
        assertThat(ids(biz("/appointments?size=50&from=" + cutoff))).containsExactly(upcoming, recent);
        assertThat(ids(biz("/appointments?size=50&to=" + cutoff))).containsExactly(old);
    }

    private List<UUID> ids(String path) throws Exception {
        List<UUID> ids = new ArrayList<>();
        data(json(get(path), owner, null).andExpect(status().isOk()))
                .forEach(node -> ids.add(UUID.fromString(node.path("id").asText())));
        return ids;
    }

    private UUID insertAppointment(int daysFromNow) {
        UUID id = UUID.randomUUID();
        UUID customerId = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", UUID.class, EMAILS[1]);
        Instant start = Instant.now().plus(daysFromNow, ChronoUnit.DAYS);
        jdbc.update("""
                INSERT INTO appointments (id, customer_id, business_id, employee_id, service_id, start_date_time,
                    end_date_time, status, price)
                VALUES (?, ?, ?::uuid, ?, ?::uuid, ?, ?, 'CONFIRMED', 100)
                """, id, customerId, businessId, employeeId, serviceId,
                Timestamp.from(start), Timestamp.from(start.plus(30, ChronoUnit.MINUTES)));
        jdbc.update("INSERT INTO appointment_services (appointment_id, service_id, service_order) VALUES (?, ?::uuid, 0)",
                id, serviceId);
        return id;
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

    private String register(String email, String role) throws Exception {
        String body = """
                {"firstName":"T","lastName":"U","email":"%s","password":"Password123!","role":"%s"}
                """.formatted(email, role);
        return data(mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())).path("accessToken").asText();
    }

    private void cleanUp() {
        for (String email : EMAILS) {
            jdbc.queryForList("SELECT id FROM users WHERE email = ?", UUID.class, email).forEach(id -> {
                String owned = "SELECT id FROM businesses WHERE owner_id = '" + id + "'";
                jdbc.update("DELETE FROM appointments WHERE business_id IN (" + owned + ")");
                jdbc.update("DELETE FROM businesses WHERE id IN (" + owned + ")");
            });
        }
        for (String email : EMAILS) {
            jdbc.update("DELETE FROM users WHERE email = ?", email);
        }
    }
}
