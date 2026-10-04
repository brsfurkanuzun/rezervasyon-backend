package com.randevupazaryeri.image.controller;

import com.randevupazaryeri.common.dto.ApiResponse;
import com.randevupazaryeri.image.dto.ImageResponse;
import com.randevupazaryeri.image.entity.ImageFolder;
import com.randevupazaryeri.image.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Images")
@SecurityRequirement(name = "bearerAuth")
public class ImageController {
    private final ImageService imageService;

    @PostMapping(value = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload an image to an allowed folder; ownerId is the business/service id (not needed for USER_AVATAR)")
    public ApiResponse<ImageResponse> upload(@RequestPart("file") MultipartFile file,
                                             @RequestParam(defaultValue = "USER_AVATAR") ImageFolder folder,
                                             @RequestParam(required = false) UUID ownerId) {
        return ApiResponse.ok(imageService.upload(folder, ownerId, file));
    }

    @DeleteMapping("/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an image (uploader, owner or admin)")
    public void delete(@PathVariable UUID imageId) {
        imageService.delete(imageId);
    }

    @PostMapping(value = "/businesses/{businessId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a business image: BUSINESS_PROFILE / BUSINESS_COVER replace the current one (owner)")
    public ApiResponse<ImageResponse> uploadBusinessImage(@PathVariable UUID businessId,
                                                          @RequestPart("file") MultipartFile file,
                                                          @RequestParam(defaultValue = "BUSINESS_GALLERY") ImageFolder folder) {
        return ApiResponse.ok(imageService.uploadBusinessImage(businessId, folder, file));
    }

    @PostMapping(value = "/businesses/{businessId}/gallery", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a gallery photo (owner or team member)")
    public ApiResponse<ImageResponse> uploadGalleryImage(@PathVariable UUID businessId,
                                                         @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(imageService.uploadBusinessImage(businessId, ImageFolder.BUSINESS_GALLERY, file));
    }

    @GetMapping("/businesses/{businessId}/images")
    @Operation(summary = "Public list of a business's images, optionally filtered by folder")
    public ApiResponse<List<ImageResponse>> listBusinessImages(@PathVariable UUID businessId,
                                                               @RequestParam(required = false) ImageFolder folder) {
        return ApiResponse.ok(imageService.listBusinessImages(businessId, folder));
    }

    @DeleteMapping("/businesses/{businessId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a business image (owner, uploader on the team, or admin)")
    public void deleteBusinessImage(@PathVariable UUID businessId, @PathVariable UUID imageId) {
        imageService.deleteBusinessImage(businessId, imageId);
    }

    @PostMapping(value = "/services/{serviceId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Set or replace a service image (business owner)")
    public ApiResponse<ImageResponse> uploadServiceImage(@PathVariable UUID serviceId,
                                                         @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(imageService.uploadServiceImage(serviceId, file));
    }

    @PostMapping(value = "/users/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Set or replace the signed-in user's profile photo")
    public ApiResponse<ImageResponse> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(imageService.uploadUserAvatar(file));
    }
}
