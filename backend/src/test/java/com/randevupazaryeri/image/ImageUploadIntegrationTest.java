package com.randevupazaryeri.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.randevupazaryeri.image.storage.ImageStorageService;
import com.randevupazaryeri.image.storage.ImageUpload;
import com.randevupazaryeri.image.storage.StorageException;
import com.randevupazaryeri.image.storage.StorageReference;
import com.randevupazaryeri.image.storage.StoredImage;
import com.randevupazaryeri.support.PostgresTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full HTTP flow against the local Postgres; the storage provider is mocked (no Cloudinary calls). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImageUploadIntegrationTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        PostgresTestSupport.registerDatasource(registry);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean ImageStorageService storage;

    String owner;
    String otherProvider;
    String customer;
    String businessId;
    String serviceId;

    @BeforeEach
    void setUp() throws Exception {
        cleanUp();
        when(storage.upload(any())).thenAnswer(inv -> {
            ImageUpload upload = inv.getArgument(0);
            String key = upload.directory() + "/" + upload.name();
            return new StoredImage(new StorageReference("cloudinary", key), "https://cdn.test/" + key + ".webp",
                    "image", 640, 480, "webp", 4096L);
        });

        owner = registerAndLogin("img-owner@test.com", "PROVIDER");
        otherProvider = registerAndLogin("img-other@test.com", "PROVIDER");
        customer = registerAndLogin("img-customer@test.com", "CUSTOMER");
        businessId = data(json(post("/api/v1/businesses"), owner, """
                {"name":"Image Salon","city":"Istanbul","district":"Kadikoy","timezone":"Europe/Istanbul"}
                """).andExpect(status().isCreated())).path("id").asText();
        serviceId = data(json(post("/api/v1/businesses/" + businessId + "/services"), owner, """
                {"name":"Cut","durationMinutes":30,"price":100,"currency":"TRY"}
                """).andExpect(status().isCreated())).path("id").asText();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void ownerUploadsCoverAndReplacingItRemovesThePreviousImage() throws Exception {
        JsonNode first = data(upload(multipart(biz("/images")).param("folder", "BUSINESS_COVER"), owner, png())
                .andExpect(status().isCreated()));
        assertThat(first.path("publicId").asText()).startsWith("resplz/businesses/" + businessId + "/cover/");
        assertThat(coverUrl()).isEqualTo(first.path("url").asText());

        JsonNode second = data(upload(multipart(biz("/images")).param("folder", "BUSINESS_COVER"), owner, png())
                .andExpect(status().isCreated()));

        verify(storage).delete(new StorageReference("cloudinary", first.path("publicId").asText()));
        assertThat(coverUrl()).isEqualTo(second.path("url").asText());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM images WHERE folder = 'BUSINESS_COVER'", Integer.class))
                .isEqualTo(1);

        mockMvc.perform(get(biz("/images")).param("folder", "BUSINESS_COVER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(second.path("id").asText()));
    }

    @Test
    void uploadsRequireAuthenticationAndOwnership() throws Exception {
        upload(multipart(biz("/gallery")), null, png()).andExpect(status().isUnauthorized());
        upload(multipart(biz("/images")).param("folder", "BUSINESS_PROFILE"), otherProvider, png())
                .andExpect(status().isForbidden());
        upload(multipart(biz("/gallery")), customer, png()).andExpect(status().isForbidden());
        upload(multipart("/api/v1/services/" + serviceId + "/image"), otherProvider, png())
                .andExpect(status().isForbidden());
        verify(storage, never()).upload(any());
    }

    @Test
    void userAvatarAndServiceImageAreLinkedToTheirOwners() throws Exception {
        JsonNode avatar = data(upload(multipart("/api/v1/users/me/avatar"), customer, png())
                .andExpect(status().isCreated()));
        assertThat(avatar.path("folder").asText()).isEqualTo("USER_AVATAR");
        assertThat(jdbc.queryForObject("SELECT photo_url FROM users WHERE email = 'img-customer@test.com'", String.class))
                .isEqualTo(avatar.path("url").asText());

        JsonNode serviceImage = data(upload(multipart("/api/v1/services/" + serviceId + "/image"), owner, png())
                .andExpect(status().isCreated()));
        mockMvc.perform(get(biz("/services")).header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].imageUrl").value(serviceImage.path("url").asText()));
    }

    @Test
    void genericUploadEndpointAcceptsOnlyKnownFolders() throws Exception {
        upload(multipart("/api/v1/images/upload").param("folder", "BUSINESS_GALLERY").param("ownerId", businessId),
                owner, png()).andExpect(status().isCreated());
        upload(multipart("/api/v1/images/upload").param("folder", "../../secrets"), owner, png())
                .andExpect(status().isBadRequest());
        upload(multipart("/api/v1/images/upload").param("folder", "BUSINESS_GALLERY"), owner, png())
                .andExpect(status().isBadRequest());
        verify(storage, times(1)).upload(any());
    }

    @Test
    void rejectsSvgSpoofedAndOversizedFiles() throws Exception {
        upload(multipart(biz("/gallery")), owner, new MockMultipartFile("file", "x.svg", "image/svg+xml", TestImages.SVG))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        upload(multipart(biz("/gallery")), owner, new MockMultipartFile("file", "x.jpg", "image/jpeg", TestImages.SVG))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IMAGE"));
        upload(multipart(biz("/gallery")), owner,
                new MockMultipartFile("file", "big.png", "image/png", new byte[5 * 1024 * 1024 + 1]))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
        mockMvc.perform(multipart(biz("/gallery")).header("Authorization", "Bearer " + owner))
                .andExpect(status().isBadRequest());
        verify(storage, never()).upload(any());
    }

    @Test
    void storageFailureReturnsGenericErrorWithoutProviderDetails() throws Exception {
        doThrow(new StorageException("cloudinary said: Invalid api_key 123")).when(storage).upload(any());

        upload(multipart(biz("/gallery")), owner, png())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("STORAGE_ERROR"))
                .andExpect(jsonPath("$.message").value("Fotoğraf yüklenirken bir hata oluştu."));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM images", Integer.class)).isZero();
    }

    @Test
    void deleteRequiresPermissionAndKeepsTheRowWhenStorageFails() throws Exception {
        String imageId = data(upload(multipart(biz("/images")).param("folder", "BUSINESS_PROFILE"), owner, png())
                .andExpect(status().isCreated())).path("id").asText();

        mockMvc.perform(delete("/api/v1/images/" + imageId)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/images/" + imageId).header("Authorization", "Bearer " + otherProvider))
                .andExpect(status().isForbidden());

        doThrow(new StorageException("down")).when(storage).delete(any());
        mockMvc.perform(delete(biz("/images/" + imageId)).header("Authorization", "Bearer " + owner))
                .andExpect(status().isInternalServerError());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM images", Integer.class)).isEqualTo(1);

        doNothing().when(storage).delete(any());
        mockMvc.perform(delete(biz("/images/" + imageId)).header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM images", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT logo_url FROM businesses WHERE id = ?::uuid", String.class, businessId))
                .isNull();
        mockMvc.perform(delete("/api/v1/images/" + imageId).header("Authorization", "Bearer " + owner))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffMemberCanAddButNotRemoveOthersGalleryImages() throws Exception {
        String ownerImage = data(upload(multipart(biz("/gallery")), owner, png())
                .andExpect(status().isCreated())).path("id").asText();
        String code = data(json(post(biz("/invitations")), owner, null).andExpect(status().isCreated()))
                .path("code").asText();
        json(post("/api/v1/invitations/" + code + "/accept"), otherProvider, null).andExpect(status().isOk());

        String staffImage = data(upload(multipart(biz("/gallery")), otherProvider, png())
                .andExpect(status().isCreated())).path("id").asText();
        verify(storage, times(2)).upload(any());

        mockMvc.perform(delete(biz("/images/" + ownerImage)).header("Authorization", "Bearer " + otherProvider))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(biz("/images/" + staffImage)).header("Authorization", "Bearer " + otherProvider))
                .andExpect(status().isNoContent());
    }

    private MockMultipartFile png() {
        return new MockMultipartFile("file", "photo.png", "image/png", TestImages.png(64, 48));
    }

    /** {@code request} must come from {@code multipart(...)}; {@code param(...)} only widens its static type. */
    private ResultActions upload(MockHttpServletRequestBuilder request, String token, MockMultipartFile file)
            throws Exception {
        ((MockMultipartHttpServletRequestBuilder) request).file(file);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
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

    private String coverUrl() {
        return jdbc.queryForObject("SELECT cover_image_url FROM businesses WHERE id = ?::uuid", String.class, businessId);
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

    private void cleanUp() {
        for (String table : new String[]{"images", "notifications", "reviews", "favorites", "appointments", "time_offs",
                "working_hours", "employee_services", "employee_invitations", "services", "employees",
                "business_categories", "businesses", "refresh_tokens", "users"}) {
            jdbc.update("DELETE FROM " + table);
        }
    }
}
