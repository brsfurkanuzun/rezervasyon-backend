package com.randevupazaryeri.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.randevupazaryeri.mail.EmailService;
import com.randevupazaryeri.support.PostgresTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetIntegrationTest {

    private static final String EMAIL = "reset-user@test.com";
    private static final String PROVIDER_EMAIL = "reset-provider@test.com";

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        PostgresTestSupport.registerDatasource(registry);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean EmailService emailService;

    String oldRefreshToken;

    @BeforeEach
    void setUp() throws Exception {
        cleanUp();
        oldRefreshToken = data(post("/api/v1/auth/register", """
                {"firstName":"Reset","lastName":"User","email":"%s","password":"Password123!","role":"CUSTOMER"}
                """.formatted(EMAIL)).andExpect(status().isCreated())).path("refreshToken").asText();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void resetsThePasswordWithTheEmailedLink() throws Exception {
        post("/api/v1/auth/password/forgot", "{\"email\":\"Reset-User@test.com\"}").andExpect(status().isOk());
        String link = sentLink(EMAIL);
        assertThat(link).startsWith("https://www.resplz.com/tr/reset-password#token=");
        String token = link.substring(link.indexOf("#token=") + 7);
        assertThat(jdbc.queryForObject("SELECT token_hash FROM password_reset_tokens", String.class))
                .isNotEqualTo(token).hasSize(64);

        post("/api/v1/auth/password/reset", body("not-the-token", "NewPassword1!"))
                .andExpect(status().isUnprocessableEntity());

        JsonNode auth = data(post("/api/v1/auth/password/reset", body(token, "NewPassword1!")).andExpect(status().isOk()));
        assertThat(auth.path("accessToken").asText()).isNotBlank();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM password_reset_tokens", Integer.class)).isZero();

        post("/api/v1/auth/login", "{\"email\":\"%s\",\"password\":\"Password123!\"}".formatted(EMAIL))
                .andExpect(status().isUnauthorized());
        post("/api/v1/auth/login", "{\"email\":\"%s\",\"password\":\"NewPassword1!\"}".formatted(EMAIL))
                .andExpect(status().isOk());
        post("/api/v1/auth/refresh", "{\"refreshToken\":\"%s\"}".formatted(oldRefreshToken))
                .andExpect(status().isUnauthorized());

        post("/api/v1/auth/password/reset", body(token, "Another1!")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void businessAccountsGetThePartnerSiteLink() throws Exception {
        post("/api/v1/auth/register", """
                {"firstName":"Reset","lastName":"Provider","email":"%s","password":"Password123!","role":"PROVIDER"}
                """.formatted(PROVIDER_EMAIL)).andExpect(status().isCreated());
        post("/api/v1/auth/password/forgot", "{\"email\":\"%s\"}".formatted(PROVIDER_EMAIL)).andExpect(status().isOk());
        assertThat(sentLink(PROVIDER_EMAIL)).startsWith("https://partner.resplz.com/tr/reset-password#token=");
    }

    @Test
    void rejectsAnExpiredLink() throws Exception {
        post("/api/v1/auth/password/forgot", "{\"email\":\"%s\"}".formatted(EMAIL)).andExpect(status().isOk());
        String link = sentLink(EMAIL);
        jdbc.update("UPDATE password_reset_tokens SET expires_at = NOW() - INTERVAL '1 minute'");
        post("/api/v1/auth/password/reset", body(link.substring(link.indexOf("#token=") + 7), "NewPassword1!"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void doesNotRevealUnknownEmails() throws Exception {
        post("/api/v1/auth/password/forgot", "{\"email\":\"nobody-here@test.com\"}").andExpect(status().isOk());
        verify(emailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void waitsBeforeSendingAnotherLink() throws Exception {
        post("/api/v1/auth/password/forgot", "{\"email\":\"%s\"}".formatted(EMAIL)).andExpect(status().isOk());
        post("/api/v1/auth/password/forgot", "{\"email\":\"%s\"}".formatted(EMAIL)).andExpect(status().isOk());
        verify(emailService, times(1)).send(eq(EMAIL), anyString(), anyString());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM password_reset_tokens", Integer.class)).isEqualTo(1);
    }

    @Test
    void endpointsArePublic() throws Exception {
        post("/api/v1/auth/password/reset", body("unknown-token", "NewPassword1!")).andExpect(status().isUnprocessableEntity());
    }

    private String sentLink(String email) {
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(emailService).send(eq(email), anyString(), text.capture());
        return text.getValue().lines().map(String::trim).filter(line -> line.startsWith("https://")).findFirst().orElseThrow();
    }

    private String body(String token, String password) {
        return "{\"token\":\"%s\",\"newPassword\":\"%s\"}".formatted(token, password);
    }

    private ResultActions post(String path, String body) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode data(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).path("data");
    }

    private void cleanUp() {
        for (String email : List.of(EMAIL, PROVIDER_EMAIL)) {
            jdbc.queryForList("SELECT id FROM users WHERE email = ?", UUID.class, email)
                    .forEach(id -> jdbc.update("DELETE FROM users WHERE id = ?", id));
        }
    }
}
