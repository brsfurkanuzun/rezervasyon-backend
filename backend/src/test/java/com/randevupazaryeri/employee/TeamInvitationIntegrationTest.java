package com.randevupazaryeri.employee;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeamInvitationIntegrationTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        PostgresTestSupport.registerDatasource(registry);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    String owner;
    String staff;
    String customer;
    String businessId;
    String serviceId;
    String ayse;
    String mehmet;

    @BeforeEach
    void setUp() throws Exception {
        for (String table : new String[]{"notifications", "reviews", "favorites", "appointments", "time_offs",
                "working_hours", "employee_services", "employee_invitations", "services", "employees",
                "business_categories", "businesses", "refresh_tokens", "users"}) {
            jdbc.update("DELETE FROM " + table);
        }
        owner = registerAndLogin("owner@test.com", "PROVIDER");
        staff = registerAndLogin("staff@test.com", "PROVIDER");
        customer = registerAndLogin("customer@test.com", "CUSTOMER");

        businessId = data(call(post("/api/v1/businesses"), owner, """
                {"name":"Team Salon","city":"Istanbul","district":"Kadikoy","timezone":"Europe/Istanbul","autoConfirm":false}
                """).andExpect(status().isCreated())).path("id").asText();
        jdbc.update("UPDATE businesses SET status = 'ACTIVE' WHERE id = ?::uuid", businessId);

        serviceId = data(call(post(biz("/services")), owner, """
                {"name":"Cut","durationMinutes":30,"price":100,"currency":"TRY"}
                """).andExpect(status().isCreated())).path("id").asText();
        ayse = createEmployee("Ayse");
        mehmet = createEmployee("Mehmet");
    }

    @Test
    void staffJoinsByInvitationAndOnlySeesAndManagesOwnWork() throws Exception {
        JsonNode invitation = data(call(post(biz("/employees/" + ayse + "/invitations")), owner,
                "{\"email\":\"staff@test.com\"}").andExpect(status().isCreated()));
        String code = invitation.path("code").asText();
        assertThat(code).matches("[A-Z2-9]{4}-[A-Z2-9]{4}");
        assertThat(invitation.path("link").asText()).isEqualTo("rezplzpartner://join/" + code.replace("-", ""));

        assertThat(data(call(get("/api/v1/invitations/mine"), staff, null)).get(0).path("code").asText()).isEqualTo(code);
        assertThat(data(call(get("/api/v1/provider/workplaces"), staff, null))).isEmpty();

        call(post("/api/v1/invitations/" + code.toLowerCase() + "/accept"), staff, null).andExpect(status().isOk());
        call(post("/api/v1/invitations/" + code + "/accept"), staff, null).andExpect(status().isNotFound());

        JsonNode workplaces = data(call(get("/api/v1/provider/workplaces"), staff, null));
        assertThat(workplaces).hasSize(1);
        assertThat(workplaces.get(0).path("role").asText()).isEqualTo("STAFF");
        assertThat(workplaces.get(0).path("employeeId").asText()).isEqualTo(ayse);
        assertThat(data(call(get("/api/v1/provider/workplaces"), owner, null)).get(0).path("role").asText())
                .isEqualTo("OWNER");

        String ayseAppointment = book(ayse, 10);
        String mehmetAppointment = book(mehmet, 11);

        assertThat(data(call(get(biz("/appointments")), staff, null))).hasSize(1);
        assertThat(data(call(get(biz("/appointments")), owner, null))).hasSize(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications n JOIN users u ON u.id = n.user_id "
                + "WHERE u.email = 'staff@test.com' AND n.type = 'APPOINTMENT_CREATED'", Integer.class)).isEqualTo(1);

        call(post(biz("/appointments/" + mehmetAppointment + "/confirm")), staff, null).andExpect(status().isNotFound());
        call(post(biz("/appointments/" + ayseAppointment + "/confirm")), staff, null).andExpect(status().isOk());

        call(put("/api/v1/businesses/" + businessId), staff, "{\"name\":\"Hacked\"}").andExpect(status().isForbidden());
        call(post(biz("/services")), staff, """
                {"name":"X","durationMinutes":30,"price":1,"currency":"TRY"}
                """).andExpect(status().isForbidden());
        call(put(biz("/employees/" + ayse + "/working-hours")), staff, "[]").andExpect(status().isForbidden());

        String timeOff = """
                {"title":"Izin","startAt":"%s","endAt":"%s"}
                """.formatted(nextWeekdayAt(0).plusSeconds(86400 * 7), nextWeekdayAt(0).plusSeconds(86400 * 8));
        call(post(biz("/employees/" + ayse + "/time-offs")), staff, timeOff).andExpect(status().isCreated());
        call(post(biz("/employees/" + mehmet + "/time-offs")), staff, timeOff).andExpect(status().isForbidden());

        call(delete(biz("/employees/" + ayse + "/account")), owner, null).andExpect(status().isOk());
        assertThat(data(call(get("/api/v1/provider/workplaces"), staff, null))).isEmpty();
        call(get(biz("/appointments")), staff, null).andExpect(status().isForbidden());
    }

    @Test
    void newInvitationRevokesThePreviousOneAndOnlyOwnerCanInvite() throws Exception {
        String first = data(call(post(biz("/employees/" + ayse + "/invitations")), owner, null)
                .andExpect(status().isCreated())).path("code").asText();
        String second = data(call(post(biz("/employees/" + ayse + "/invitations")), owner, null)
                .andExpect(status().isCreated())).path("code").asText();

        call(get("/api/v1/invitations/" + first), staff, null).andExpect(status().isNotFound());
        call(get("/api/v1/invitations/" + second), staff, null).andExpect(status().isOk());
        call(post(biz("/employees/" + mehmet + "/invitations")), staff, null).andExpect(status().isForbidden());
        call(post("/api/v1/invitations/" + second + "/accept"), customer, null).andExpect(status().is4xxClientError());
        call(post("/api/v1/invitations/" + second + "/accept"), owner, null).andExpect(status().is4xxClientError());
    }

    @Test
    void ownerWorksAsExpertAndBusinessAccountsAlsoActAsCustomers() throws Exception {
        call(post(biz("/employees/" + ayse + "/account/self")), owner, null).andExpect(status().isOk());
        call(post(biz("/employees/" + mehmet + "/account/self")), owner, null).andExpect(status().is4xxClientError());

        JsonNode workplace = data(call(get("/api/v1/provider/workplaces"), owner, null)).get(0);
        assertThat(workplace.path("role").asText()).isEqualTo("OWNER");
        assertThat(workplace.path("employeeId").asText()).isEqualTo(ayse);

        book(ayse, 10);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications n JOIN users u ON u.id = n.user_id "
                + "WHERE u.email = 'owner@test.com' AND n.type = 'APPOINTMENT_CREATED'", Integer.class)).isEqualTo(1);

        String asOwner = objectMapper.writeValueAsString(Map.of(
                "businessId", businessId, "employeeId", ayse, "serviceId", serviceId,
                "startDateTime", nextWeekdayAt(12).toString()));
        call(post("/api/v1/appointments"), owner, asOwner).andExpect(status().isUnprocessableEntity());

        String withColleague = objectMapper.writeValueAsString(Map.of(
                "businessId", businessId, "employeeId", mehmet, "serviceId", serviceId,
                "startDateTime", nextWeekdayAt(12).toString()));
        call(post("/api/v1/appointments"), staff, withColleague).andExpect(status().isCreated());
        assertThat(data(call(get("/api/v1/appointments/my"), staff, null))).hasSize(1);
        call(post("/api/v1/favorites/" + businessId), staff, null).andExpect(status().isCreated());
    }

    @Test
    void customerCanUpgradeToBusinessAccountWithSameLogin() throws Exception {
        call(get("/api/v1/provider/workplaces"), customer, null)
                .andExpect(status().isForbidden());

        JsonNode upgraded = data(call(post("/api/v1/auth/upgrade-to-provider"), customer, null)
                .andExpect(status().isOk()));
        assertThat(upgraded.path("user").path("role").asText()).isEqualTo("PROVIDER");
        String token = upgraded.path("accessToken").asText();

        call(get("/api/v1/provider/workplaces"), token, null).andExpect(status().isOk());
        call(get("/api/v1/appointments/my"), token, null).andExpect(status().isOk());
    }

    private String createEmployee(String name) throws Exception {
        String id = data(call(post(biz("/employees")), owner,
                "{\"firstName\":\"" + name + "\",\"lastName\":\"Y\",\"serviceIds\":[\"" + serviceId + "\"]}")
                .andExpect(status().isCreated())).path("id").asText();
        StringBuilder days = new StringBuilder("[");
        for (int d = 1; d <= 7; d++) {
            days.append(d > 1 ? "," : "")
                    .append("{\"dayOfWeek\":").append(d).append(",\"startTime\":\"09:00:00\",\"endTime\":\"18:00:00\"}");
        }
        call(put(biz("/employees/" + id + "/working-hours")), owner, days.append("]").toString())
                .andExpect(status().isOk());
        return id;
    }

    private String book(String employeeId, int hour) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "businessId", businessId, "employeeId", employeeId, "serviceId", serviceId,
                "startDateTime", nextWeekdayAt(hour).toString()));
        return data(call(post("/api/v1/appointments"), customer, body).andExpect(status().isCreated()))
                .path("id").asText();
    }

    private String biz(String path) {
        return "/api/v1/businesses/" + businessId + path;
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String token, String body) throws Exception {
        request.header("Authorization", "Bearer " + token);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }

    private JsonNode data(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).path("data");
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

    private Instant nextWeekdayAt(int hour) {
        ZoneId zone = ZoneId.of("Europe/Istanbul");
        LocalDate date = LocalDate.now(zone).plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date.atTime(hour, 0).atZone(zone).toInstant();
    }
}
