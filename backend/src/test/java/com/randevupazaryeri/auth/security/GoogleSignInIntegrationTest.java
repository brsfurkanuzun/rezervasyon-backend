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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoogleSignInIntegrationTest {
    private static final String CLIENT_ID = "test-web-client.apps.googleusercontent.com";

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

        jdbc.update("DELETE FROM users WHERE email IN (?, ?, ?)",
                "google.new@gmail.com", "google.link@gmail.com", "google-owner@example.com");
    }

    @Test
    void exposesTheWebClientId() throws Exception {
        mockMvc.perform(get("/api/v1/auth/google/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientId").value(CLIENT_ID));
    }

    @Test
    void createsAccountOnFirstSignInAndReusesItAfterwards() throws Exception {
        JsonNode first = signIn(token("google-sub-new", "google.new@gmail.com", null, CLIENT_ID, googleKey), "");
        assertThat(first.path("user").path("firstName").asText()).isEqualTo("Ayşe");
        assertThat(first.path("user").path("lastName").asText()).isEqualTo("Yılmaz");
        assertThat(first.path("user").path("role").asText()).isEqualTo("CUSTOMER");

        JsonNode second = signIn(token("google-sub-new", "google.new@gmail.com", null, CLIENT_ID, googleKey), "");
        assertThat(second.path("user").path("id").asText()).isEqualTo(first.path("user").path("id").asText());
    }

    @Test
    void fillsInThePlaceholderNameOfAnExistingAccount() throws Exception {
        signIn(token("google-sub-new", "google.new@gmail.com", null, CLIENT_ID, googleKey), "");
        jdbc.update("UPDATE users SET first_name = 'rezplz', last_name = 'Kullanıcısı' WHERE email = ?",
                "google.new@gmail.com");

        JsonNode data = signIn(token("google-sub-new", "google.new@gmail.com", null, CLIENT_ID, googleKey), "");
        assertThat(data.path("user").path("firstName").asText()).isEqualTo("Ayşe");
        assertThat(data.path("user").path("lastName").asText()).isEqualTo("Yılmaz");
    }

    @Test
    void usesTheFullNameClaimWhenGivenAndFamilyNameAreMissing() throws Exception {
        Instant now = Instant.now();
        String idToken = Jwts.builder()
                .header().keyId("test-kid").and()
                .issuer("https://accounts.google.com")
                .audience().add(CLIENT_ID).and()
                .subject("google-sub-new")
                .claim("email", "google.new@gmail.com")
                .claim("email_verified", true)
                .claim("name", "Ayşe Nur Yılmaz")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(600)))
                .signWith(googleKey, Jwts.SIG.RS256)
                .compact();

        JsonNode data = signIn(idToken, "");
        assertThat(data.path("user").path("firstName").asText()).isEqualTo("Ayşe Nur");
        assertThat(data.path("user").path("lastName").asText()).isEqualTo("Yılmaz");
    }

    @Test
    void linksExistingPasswordAccountWhenGoogleOwnsTheEmail() throws Exception {
        register("google.link@gmail.com");

        JsonNode data = signIn(token("google-sub-link", "google.link@gmail.com", null, CLIENT_ID, googleKey),
                "\"role\":\"CUSTOMER\",");

        assertThat(data.path("user").path("email").asText()).isEqualTo("google.link@gmail.com");
        assertThat(data.path("user").path("role").asText()).isEqualTo("PROVIDER");
        assertThat(jdbc.queryForObject("SELECT google_user_id FROM users WHERE email = ?", String.class,
                "google.link@gmail.com")).isEqualTo("google-sub-link");
    }

    @Test
    void refusesToTakeOverAccountsWhoseEmailGoogleDoesNotOwn() throws Exception {
        register("google-owner@example.com");

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"" + token("google-sub-x", "google-owner@example.com", null, CLIENT_ID, googleKey) + "\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("SELECT google_user_id FROM users WHERE email = ?", String.class,
                "google-owner@example.com")).isNull();
    }

    @Test
    void rejectsTokensForOtherAppsOrWithForgedSignature() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        PrivateKey forger = generator.generateKeyPair().getPrivate();

        for (String token : new String[]{
                token("google-sub-x", "google.new@gmail.com", null, "someone-else.apps.googleusercontent.com", googleKey),
                token("google-sub-x", "google.new@gmail.com", null, CLIENT_ID, forger),
                token("google-sub-x", "google.new@gmail.com", "https://evil.example.com", CLIENT_ID, googleKey)}) {
            mockMvc.perform(post("/api/v1/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idToken\":\"" + token + "\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Link","lastName":"User","email":"%s","password":"Password123!","role":"PROVIDER"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private JsonNode signIn(String idToken, String extraFields) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + extraFields + "\"idToken\":\"" + idToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data");
    }

    private static String token(String subject, String email, String issuer, String audience, PrivateKey key) {
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId("test-kid").and()
                .issuer(issuer != null ? issuer : "https://accounts.google.com")
                .audience().add(audience).and()
                .subject(subject)
                .claim("email", email)
                .claim("email_verified", true)
                .claim("given_name", "Ayşe")
                .claim("family_name", "Yılmaz")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(600)))
                .signWith(key, Jwts.SIG.RS256)
                .compact();
    }
}
