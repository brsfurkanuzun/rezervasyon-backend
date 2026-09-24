package com.randevupazaryeri.address;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.randevupazaryeri.support.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAddressIntegrationTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        PostgresTestSupport.registerDatasource(registry);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void customerCanCreateAndListOwnAddress() throws Exception {
        String email = "address-test@test.com";
        MvcResult register = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Address","lastName":"Tester","email":"%s","password":"Password123!","role":"CUSTOMER"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode auth = objectMapper.readTree(register.getResponse().getContentAsString()).path("data");
        String access = auth.path("accessToken").asText();

        mockMvc.perform(post("/api/v1/account/addresses")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label":"Ev","recipientName":"Address Tester","phone":"5551112233","addressLine":"Moda Cad. No:1","city":"Istanbul","district":"Kadikoy","postalCode":"34710","isDefault":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.label").value("Ev"));

        mockMvc.perform(get("/api/v1/account/addresses")
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].addressLine").value("Moda Cad. No:1"));
    }
}
