package com.randevupazaryeri.auth.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.randevupazaryeri.support.PostgresTestSupport;
import io.jsonwebtoken.Jwts;
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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountSettingsIntegrationTest {
    private static final String CLIENT_ID = "test-web-client.apps.googleusercontent.com";
    private static final String PASSWORD_USER = "settings.password@example.com";
    private static final String GOOGLE_USER = "settings.google@gmail.com";

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        PostgresTestSupport.registerDatasource(registry);
        registry.add("app.google.web-client-id", () -> CLIENT_ID);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired GoogleIdentityVerifier verifier;

    private PrivateKey googleKey;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        googleKey = pair.getPrivate();
        verifier.useKeys(Map.of("test-kid", pair.getPublic()));
        jdbc.update("DELETE FROM users WHERE email IN (?, ?)", PASSWORD_USER, GOOGLE_USER);
    }

    @Test
    void changesPasswordOnlyWithTheCurrentOne() throws Exception {
        String access = register(PASSWORD_USER).path("accessToken").asText();

        mockMvc.perform(put("/api/v1/auth/me/password").header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong-password\",\"newPassword\":\"NewPassword456!\"}"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(put("/api/v1/auth/me/password").header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Password123!\",\"newPassword\":\"NewPassword456!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + PASSWORD_USER + "\",\"password\":\"NewPassword456!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void passwordLoginMarksTheAccountAsHavingAPassword() throws Exception {
        register(PASSWORD_USER);
        jdbc.update("UPDATE users SET password_set = FALSE WHERE email = ?", PASSWORD_USER);

        String access = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + PASSWORD_USER + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.hasPassword").value(true))
                .andReturn().getResponse().getContentAsString()).path("data").path("accessToken").asText();

        mockMvc.perform(put("/api/v1/auth/me/password").header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"NewPassword456!\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void socialAccountCannotUnlinkItsOnlySignInMethodUntilItSetsAPassword() throws Exception {
        String access = googleSignIn("google-sub-settings", GOOGLE_USER).path("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(jsonPath("$.data.hasPassword").value(false))
                .andExpect(jsonPath("$.data.googleLinked").value(true))
                .andExpect(jsonPath("$.data.appleLinked").value(false));

        mockMvc.perform(delete("/api/v1/auth/me/google").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnprocessableEntity());

        String fresh = objectMapper.readTree(mockMvc.perform(put("/api/v1/auth/me/password")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"MyOwnPassword1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.hasPassword").value(true))
                .andReturn().getResponse().getContentAsString()).path("data").path("accessToken").asText();

        mockMvc.perform(delete("/api/v1/auth/me/google").header("Authorization", "Bearer " + fresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.googleLinked").value(false));
    }

    @Test
    void linksGoogleUnlessAnotherAccountOwnsIt() throws Exception {
        googleSignIn("google-sub-taken", GOOGLE_USER);
        String access = register(PASSWORD_USER).path("accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/me/google").header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"" + token("google-sub-taken", GOOGLE_USER) + "\"}"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/v1/auth/me/google").header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"" + token("google-sub-free", "someone@gmail.com") + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.googleLinked").value(true));
        assertThat(jdbc.queryForObject("SELECT google_user_id FROM users WHERE email = ?", String.class,
                PASSWORD_USER)).isEqualTo("google-sub-free");
    }

    @Test
    void profileUpdateRejectsAnotherAccountsEmailOrPhone() throws Exception {
        String other = register(GOOGLE_USER).path("accessToken").asText();
        updateProfile(other, GOOGLE_USER, "+905550001122").andExpect(status().isOk());
        String access = register(PASSWORD_USER).path("accessToken").asText();

        updateProfile(access, PASSWORD_USER, "+905550001122")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Phone number already registered"));
        updateProfile(access, GOOGLE_USER, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Email already registered"));
        updateProfile(access, PASSWORD_USER, "+905550003344").andExpect(status().isOk());
        updateProfile(access, PASSWORD_USER, "+905550003344")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("+905550003344"));
    }

    private ResultActions updateProfile(String access, String email, String phone) throws Exception {
        String phoneJson = phone == null ? "null" : "\"" + phone + "\"";
        return mockMvc.perform(put("/api/v1/auth/me").header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName":"Yeni","lastName":"İsim","email":"%s","phone":%s}
                        """.formatted(email, phoneJson)));
    }

    private JsonNode register(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Set","lastName":"Tings","email":"%s","password":"Password123!","role":"CUSTOMER"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data");
    }

    private JsonNode googleSignIn(String subject, String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"" + token(subject, email) + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data");
    }

    private String token(String subject, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId("test-kid").and()
                .issuer("https://accounts.google.com")
                .audience().add(CLIENT_ID).and()
                .subject(subject)
                .claim("email", email)
                .claim("email_verified", true)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(600)))
                .signWith(googleKey, Jwts.SIG.RS256)
                .compact();
    }
}
