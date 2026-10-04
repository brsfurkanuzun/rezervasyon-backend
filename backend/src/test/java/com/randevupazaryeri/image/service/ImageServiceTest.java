package com.randevupazaryeri.image.service;

import com.randevupazaryeri.business.repository.BusinessRepository;
import com.randevupazaryeri.common.exception.ForbiddenException;
import com.randevupazaryeri.common.exception.ImageValidationException;
import com.randevupazaryeri.common.exception.ResourceNotFoundException;
import com.randevupazaryeri.config.UploadProperties;
import com.randevupazaryeri.image.dto.ImageResponse;
import com.randevupazaryeri.image.entity.Image;
import com.randevupazaryeri.image.entity.ImageFolder;
import com.randevupazaryeri.image.entity.ImageOwnerType;
import com.randevupazaryeri.image.repository.ImageRepository;
import com.randevupazaryeri.image.storage.ImageStorageService;
import com.randevupazaryeri.image.storage.ImageUpload;
import com.randevupazaryeri.image.storage.StorageException;
import com.randevupazaryeri.image.storage.StorageReference;
import com.randevupazaryeri.image.storage.StoredImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ImageServiceTest {

    ImageStorageService storage = mock(ImageStorageService.class);
    ImageRepository imageRepository = mock(ImageRepository.class);
    BusinessRepository businessRepository = mock(BusinessRepository.class);
    ImageFileValidator validator = mock(ImageFileValidator.class);
    UploadRateLimiter rateLimiter = mock(UploadRateLimiter.class);
    ImageAccessPolicy access = mock(ImageAccessPolicy.class);
    ImageOwnerLinker linker = mock(ImageOwnerLinker.class);
    ImageService service;

    UUID userId = UUID.randomUUID();
    UUID businessId = UUID.randomUUID();
    MockMultipartFile file = new MockMultipartFile("file", "../../etc/passwd.png", "image/png", new byte[]{1});

    @BeforeEach
    void setUp() {
        service = new ImageService(storage, imageRepository, businessRepository, validator, rateLimiter, access,
                linker, new UploadProperties(), new TransactionTemplate(mock(PlatformTransactionManager.class)));
        when(access.currentUserId()).thenReturn(userId);
        when(validator.validate(any())).thenReturn(new ImageFileValidator.ValidatedImage(new byte[]{1}, "image/png"));
        when(storage.upload(any())).thenAnswer(inv -> stored(inv.getArgument(0)));
        when(imageRepository.save(any(Image.class))).thenAnswer(inv -> {
            Image image = inv.getArgument(0);
            image.setId(UUID.randomUUID());
            return image;
        });
    }

    @Test
    void validUploadStoresUnderServerBuiltPathAndPersistsMetadata() {
        ImageResponse response = service.uploadBusinessImage(businessId, ImageFolder.BUSINESS_COVER, file);

        ArgumentCaptor<ImageUpload> upload = ArgumentCaptor.forClass(ImageUpload.class);
        verify(storage).upload(upload.capture());
        assertThat(upload.getValue().directory()).isEqualTo("resplz/businesses/" + businessId + "/cover");
        assertThat(upload.getValue().name()).doesNotContain("passwd").doesNotContain("..");
        assertThat(upload.getValue().maxWidth()).isEqualTo(1600);

        ArgumentCaptor<Image> saved = ArgumentCaptor.forClass(Image.class);
        verify(imageRepository).save(saved.capture());
        Image image = saved.getValue();
        assertThat(image.getStorageProvider()).isEqualTo("test");
        assertThat(image.getPublicId()).startsWith("resplz/businesses/" + businessId + "/cover/");
        assertThat(image.getFolder()).isEqualTo(ImageFolder.BUSINESS_COVER);
        assertThat(image.getOwnerType()).isEqualTo(ImageOwnerType.BUSINESS);
        assertThat(image.getOwnerId()).isEqualTo(businessId);
        assertThat(image.getUploadedBy()).isEqualTo(userId);
        assertThat(image.getWidth()).isEqualTo(800);
        assertThat(image.getFormat()).isEqualTo("webp");
        assertThat(image.getBytes()).isEqualTo(2048L);

        verify(linker).link(ImageFolder.BUSINESS_COVER, businessId, image.getUrl());
        verify(rateLimiter).acquire(userId);
        assertThat(response.getUrl()).isEqualTo(image.getUrl());
        assertThat(response.getThumbnailUrl()).isEqualTo(image.getUrl());
    }

    @Test
    void unauthorizedUploadNeverReachesStorage() {
        doThrow(new ForbiddenException("no")).when(access).checkCanUpload(ImageFolder.BUSINESS_PROFILE, businessId);

        assertThatThrownBy(() -> service.uploadBusinessImage(businessId, ImageFolder.BUSINESS_PROFILE, file))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(storage, validator);
        verify(imageRepository, never()).save(any());
    }

    @Test
    void invalidFileNeverReachesStorage() {
        when(validator.validate(any())).thenThrow(ImageValidationException.unsupportedType("svg"));

        assertThatThrownBy(() -> service.uploadUserAvatar(file)).isInstanceOf(ImageValidationException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void avatarAlwaysTargetsTheCurrentUser() {
        service.upload(ImageFolder.USER_AVATAR, UUID.randomUUID(), file);

        verify(access).checkCanUpload(ImageFolder.USER_AVATAR, userId);
        verify(linker).link(eq(ImageFolder.USER_AVATAR), eq(userId), any());
    }

    @Test
    void storageUploadFailurePersistsNothingAndKeepsTheOldImage() {
        doThrow(new StorageException("down")).when(storage).upload(any());

        assertThatThrownBy(() -> service.uploadBusinessImage(businessId, ImageFolder.BUSINESS_PROFILE, file))
                .isInstanceOf(StorageException.class);
        verify(imageRepository, never()).save(any());
        verifyNoInteractions(linker);
        verify(storage, never()).delete(any());
    }

    @Test
    void metadataFailureRemovesTheJustUploadedAsset() {
        when(imageRepository.save(any(Image.class))).thenThrow(new DataIntegrityViolationException("db"));

        assertThatThrownBy(() -> service.uploadBusinessImage(businessId, ImageFolder.BUSINESS_GALLERY, file))
                .isInstanceOf(DataIntegrityViolationException.class);
        ArgumentCaptor<StorageReference> deleted = ArgumentCaptor.forClass(StorageReference.class);
        verify(storage).delete(deleted.capture());
        assertThat(deleted.getValue().key()).startsWith("resplz/businesses/" + businessId + "/gallery/");
    }

    @Test
    void replacingDeletesThePreviousImageOnlyAfterTheNewOneIsLinked() {
        Image old = image(ImageFolder.BUSINESS_PROFILE, businessId, null);
        when(imageRepository.findByOwnerTypeAndOwnerIdAndFolderAndIdNot(eq(ImageOwnerType.BUSINESS), eq(businessId),
                eq(ImageFolder.BUSINESS_PROFILE), any())).thenReturn(List.of(old));

        service.uploadBusinessImage(businessId, ImageFolder.BUSINESS_PROFILE, file);

        InOrder order = inOrder(storage, linker, imageRepository);
        order.verify(storage).upload(any());
        order.verify(linker).link(eq(ImageFolder.BUSINESS_PROFILE), eq(businessId), any());
        order.verify(storage).delete(new StorageReference("test", old.getPublicId()));
        order.verify(imageRepository).deleteById(old.getId());
    }

    @Test
    void failedCleanupOfReplacedImageKeepsItsRowForRetry() {
        Image old = image(ImageFolder.USER_AVATAR, userId, userId);
        when(imageRepository.findByOwnerTypeAndOwnerIdAndFolderAndIdNot(any(), any(), any(), any())).thenReturn(List.of(old));
        doThrow(new StorageException("down")).when(storage).delete(new StorageReference("test", old.getPublicId()));

        ImageResponse response = service.uploadUserAvatar(file);

        assertThat(response.getId()).isNotNull();
        verify(imageRepository, never()).deleteById(old.getId());
    }

    @Test
    void galleryUploadsDoNotReplaceAnything() {
        service.uploadBusinessImage(businessId, ImageFolder.BUSINESS_GALLERY, file);

        verify(imageRepository, never()).findByOwnerTypeAndOwnerIdAndFolderAndIdNot(any(), any(), any(), any());
        verify(storage, never()).delete(any());
    }

    @Test
    void deleteRemovesFromStorageBeforeTheDatabase() {
        Image image = image(ImageFolder.BUSINESS_COVER, businessId, userId);
        when(imageRepository.findById(image.getId())).thenReturn(Optional.of(image));

        service.delete(image.getId());

        InOrder order = inOrder(access, storage, linker, imageRepository);
        order.verify(access).checkCanDelete(image);
        order.verify(storage).delete(new StorageReference("test", image.getPublicId()));
        order.verify(linker).unlinkIfCurrent(ImageFolder.BUSINESS_COVER, businessId, image.getUrl());
        order.verify(imageRepository).deleteById(image.getId());
    }

    @Test
    void storageDeleteFailureKeepsTheDatabaseRow() {
        Image image = image(ImageFolder.BUSINESS_GALLERY, businessId, userId);
        when(imageRepository.findById(image.getId())).thenReturn(Optional.of(image));
        doThrow(new StorageException("down")).when(storage).delete(any());

        assertThatThrownBy(() -> service.delete(image.getId())).isInstanceOf(StorageException.class);
        verify(imageRepository, never()).deleteById(any());
        verifyNoInteractions(linker);
    }

    @Test
    void unauthorizedDeleteTouchesNothing() {
        Image image = image(ImageFolder.BUSINESS_GALLERY, businessId, UUID.randomUUID());
        when(imageRepository.findById(image.getId())).thenReturn(Optional.of(image));
        doThrow(new ForbiddenException("no")).when(access).checkCanDelete(image);

        assertThatThrownBy(() -> service.delete(image.getId())).isInstanceOf(ForbiddenException.class);
        verify(storage, never()).delete(any());
        verify(imageRepository, never()).deleteById(any());
    }

    @Test
    void deletingThroughAnotherBusinessIsNotFound() {
        Image image = image(ImageFolder.BUSINESS_GALLERY, UUID.randomUUID(), userId);
        when(imageRepository.findById(image.getId())).thenReturn(Optional.of(image));

        assertThatThrownBy(() -> service.deleteBusinessImage(businessId, image.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsNonBusinessFolderOnBusinessEndpointsAndMissingOwner() {
        assertThatThrownBy(() -> service.uploadBusinessImage(businessId, ImageFolder.USER_AVATAR, file))
                .isInstanceOf(ImageValidationException.class);
        assertThatThrownBy(() -> service.upload(ImageFolder.SERVICE_IMAGE, null, file))
                .isInstanceOf(ImageValidationException.class);
        verifyNoInteractions(storage);
    }

    private static StoredImage stored(ImageUpload upload) {
        String key = upload.directory() + "/" + upload.name();
        return new StoredImage(new StorageReference("test", key), "https://cdn.test/" + key + ".webp",
                "image", 800, 600, "webp", 2048L);
    }

    private static Image image(ImageFolder folder, UUID ownerId, UUID uploadedBy) {
        UUID id = UUID.randomUUID();
        return Image.builder().id(id).storageProvider("test").publicId("resplz/x/" + id)
                .url("https://cdn.test/" + id + ".webp").resourceType("image").folder(folder)
                .ownerType(folder.ownerType()).ownerId(ownerId).uploadedBy(uploadedBy).build();
    }
}
