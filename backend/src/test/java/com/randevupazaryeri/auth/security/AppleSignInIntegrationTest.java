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
class AppleSignInIntegrationTest {
    private static final String WEB_SERVICE_ID = "com.example.web.test";

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        PostgresTestSupport.registerDatasource(registry);
        registry.add("app.apple.web-service-id", () -> WEB_SERVICE_ID);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired AppleIdentityVerifier verifier;

    private PrivateKey appleKey;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        appleKey = pair.getPrivate();
        verifier.useKeys(Map.of("test-kid", pair.getPublic()));

        jdbc.update("DELETE FROM users WHERE email IN (?, ?)", "apple-new@test.com", "apple-link@test.com");
    }

    @Test
    void createsAccountOnFirstSignInAndReusesItAfterwards() throws Exception {
        JsonNode first = signIn(token("apple-sub-new", "apple-new@test.com", "com.rezplz.rezplz", appleKey),
                "\"firstName\":\"Ayşe\",\"lastName\":\"Yılmaz\",");
        assertThat(first.path("user").path("firstName").asText()).isEqualTo("Ayşe");
        assertThat(first.path("user").path("role").asText()).isEqualTo("CUSTOMER");

        JsonNode second = signIn(token("apple-sub-new", null, "com.rezplz.rezplz", appleKey), "");
        assertThat(second.path("user").path("id").asText()).isEqualTo(first.path("user").path("id").asText());
        assertThat(second.path("user").path("lastName").asText()).isEqualTo("Yılmaz");
    }

    @Test
    void exposesTheWebServiceIdAndAcceptsTokensIssuedForIt() throws Exception {
        mockMvc.perform(get("/api/v1/auth/apple/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientId").value(WEB_SERVICE_ID));

        JsonNode iosUser = signIn(token("apple-sub-new", "apple-new@test.com", "com.rezplz.rezplz", appleKey), "");
        JsonNode webUser = signIn(token("apple-sub-new", null, WEB_SERVICE_ID, appleKey), "");
        assertThat(webUser.path("user").path("id").asText()).isEqualTo(iosUser.path("user").path("id").asText());
    }

    @Test
    void linksExistingPasswordAccountWithSameVerifiedEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Link","lastName":"User","email":"apple-link@test.com","password":"Password123!","role":"PROVIDER"}
                                """))
                .andExpect(status().isCreated());

        JsonNode data = signIn(token("apple-sub-link", "apple-link@test.com", "com.rezplz.partner", appleKey),
                "\"role\":\"PROVIDER\",");

        assertThat(data.path("user").path("email").asText()).isEqualTo("apple-link@test.com");
        assertThat(data.path("user").path("role").asText()).isEqualTo("PROVIDER");
        assertThat(jdbc.queryForObject("SELECT apple_user_id FROM users WHERE email = ?", String.class,
                "apple-link@test.com")).isEqualTo("apple-sub-link");
    }

    @Test
    void rejectsTokensForOtherAppsOrWithForgedSignature() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        PrivateKey forger = generator.generateKeyPair().getPrivate();

        for (String token : new String[]{
                token("apple-sub-x", "apple-new@test.com", "com.someone.else", appleKey),
                token("apple-sub-x", "apple-new@test.com", "com.rezplz.rezplz", forger)}) {
            mockMvc.perform(post("/api/v1/auth/apple")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"identityToken\":\"" + token + "\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    private JsonNode signIn(String identityToken, String extraFields) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + extraFields + "\"identityToken\":\"" + identityToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data");
    }

    private static String token(String subject, String email, String audience, PrivateKey key) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .header().keyId("test-kid").and()
                .issuer("https://appleid.apple.com")
                .audience().add(audience).and()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(600)));
        if (email != null) {
            builder.claim("email", email).claim("email_verified", "true");
        }
        return builder.signWith(key, Jwts.SIG.RS256).compact();
    }
}
