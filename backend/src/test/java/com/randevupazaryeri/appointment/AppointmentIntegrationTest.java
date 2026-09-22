package com.randevupazaryeri.appointment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.randevupazaryeri.support.PostgresTestSupport;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppointmentIntegrationTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        PostgresTestSupport.registerDatasource(registry);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    UUID businessId;
    UUID employeeId;
    UUID serviceId;
    String customerToken;
    String providerToken;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("DELETE FROM notifications");
        jdbc.update("DELETE FROM reviews");
        jdbc.update("DELETE FROM favorites");
        jdbc.update("DELETE FROM appointments");
        jdbc.update("DELETE FROM time_offs");
        jdbc.update("DELETE FROM working_hours");
        jdbc.update("DELETE FROM employee_services");
        jdbc.update("DELETE FROM services");
        jdbc.update("DELETE FROM employees");
        jdbc.update("DELETE FROM business_categories");
        jdbc.update("DELETE FROM businesses");
        jdbc.update("DELETE FROM refresh_tokens");
        jdbc.update("DELETE FROM users");

        register("cust@test.com", "CUSTOMER");
        register("prov@test.com", "PROVIDER");
        customerToken = login("cust@test.com");
        providerToken = login("prov@test.com");

        MvcResult biz = mockMvc.perform(post("/api/v1/businesses")
                        .header("Authorization", "Bearer " + providerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test Salon","city":"Istanbul","district":"Kadikoy","timezone":"Europe/Istanbul","autoConfirm":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        businessId = UUID.fromString(objectMapper.readTree(biz.getResponse().getContentAsString()).path("data").path("id").asText());

        jdbc.update("UPDATE businesses SET status = 'ACTIVE' WHERE id = ?", businessId);

        String catId = jdbc.queryForObject("SELECT id::text FROM categories WHERE code = 'BEAUTY_SALON'", String.class);
        jdbc.update("INSERT INTO business_categories(business_id, category_id) VALUES (?, ?::uuid)", businessId, catId);

        MvcResult svc = mockMvc.perform(post("/api/v1/businesses/" + businessId + "/services")
                        .header("Authorization", "Bearer " + providerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Cut","durationMinutes":30,"price":100,"currency":"TRY"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        serviceId = UUID.fromString(objectMapper.readTree(svc.getResponse().getContentAsString()).path("data").path("id").asText());

        MvcResult emp = mockMvc.perform(post("/api/v1/businesses/" + businessId + "/employees")
                        .header("Authorization", "Bearer " + providerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ayse\",\"lastName\":\"Y\",\"serviceIds\":[\"" + serviceId + "\"]}")
                )
                .andExpect(status().isCreated())
                .andReturn();
        employeeId = UUID.fromString(objectMapper.readTree(emp.getResponse().getContentAsString()).path("data").path("id").asText());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/businesses/" + businessId + "/employees/" + employeeId + "/working-hours")
                        .header("Authorization", "Bearer " + providerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"dayOfWeek":1,"startTime":"09:00:00","endTime":"18:00:00","isAvailable":true},
                                  {"dayOfWeek":2,"startTime":"09:00:00","endTime":"18:00:00","isAvailable":true},
                                  {"dayOfWeek":3,"startTime":"09:00:00","endTime":"18:00:00","isAvailable":true},
                                  {"dayOfWeek":4,"startTime":"09:00:00","endTime":"18:00:00","isAvailable":true},
                                  {"dayOfWeek":5,"startTime":"09:00:00","endTime":"18:00:00","isAvailable":true}
                                ]
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void createsValidAppointment() throws Exception {
        Instant start = nextWeekdayAt(10, 0);
        book(start, customerToken).andExpect(status().isCreated());
    }

    @Test
    void rejectsPastAppointment() throws Exception {
        Instant start = Instant.now().minus(Duration.ofHours(1));
        book(start, customerToken).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rejectsOutsideWorkingHours() throws Exception {
        Instant start = nextWeekdayAt(20, 0);
        book(start, customerToken).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rejectsConflict() throws Exception {
        Instant start = nextWeekdayAt(11, 0);
        book(start, customerToken).andExpect(status().isCreated());
        book(start, customerToken).andExpect(status().isConflict());
    }

    @Test
    void concurrentBookingOnlyOneSucceeds() throws Exception {
        Instant start = nextWeekdayAt(12, 0);
        String body = objectMapper.writeValueAsString(Map.of(
                "businessId", businessId,
                "employeeId", employeeId,
                "serviceId", serviceId,
                "startDateTime", start.toString()
        ));
        AtomicInteger created = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                try {
                    ready.await();
                    int status = mockMvc.perform(post("/api/v1/appointments")
                                    .header("Authorization", "Bearer " + customerToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                            .andReturn().getResponse().getStatus();
                    if (status == 201) created.incrementAndGet();
                    if (status == 409) conflicts.incrementAndGet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    done.countDown();
                }
            });
        }
        ready.countDown();
        done.await();
        pool.shutdown();
        pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(created.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(1);
    }

    private org.springframework.test.web.servlet.ResultActions book(Instant start, String token) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "businessId", businessId,
                "employeeId", employeeId,
                "serviceId", serviceId,
                "startDateTime", start.toString()
        ));
        return mockMvc.perform(post("/api/v1/appointments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void register(String email, String role) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"T","lastName":"U","email":"%s","password":"Password123!","role":"%s"}
                                """.formatted(email, role)))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Password123!\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.path("data").path("accessToken").asText();
    }

    private Instant nextWeekdayAt(int hour, int minute) {
        ZoneId zone = ZoneId.of("Europe/Istanbul");
        LocalDate date = LocalDate.now(zone).plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date.atTime(hour, minute).atZone(zone).toInstant();
    }
}
