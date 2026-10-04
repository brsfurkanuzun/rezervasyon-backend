package com.randevupazaryeri.image.service;

import com.randevupazaryeri.business.repository.BusinessRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Image use cases: authorize → validate → store → persist metadata → link to the owner.
 * No database transaction is held open while talking to the storage provider.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    static final int THUMBNAIL_WIDTH = 400;

    private final ImageStorageService storage;
    private final ImageRepository imageRepository;
    private final BusinessRepository businessRepository;
    private final ImageFileValidator validator;
    private final UploadRateLimiter rateLimiter;
    private final ImageAccessPolicy access;
    private final ImageOwnerLinker linker;
    private final UploadProperties properties;
    private final TransactionTemplate transactionTemplate;

    public ImageResponse uploadUserAvatar(MultipartFile file) {
        return upload(ImageFolder.USER_AVATAR, null, file);
    }

    public ImageResponse uploadBusinessImage(UUID businessId, ImageFolder folder, MultipartFile file) {
        requireBusinessFolder(folder);
        return upload(folder, businessId, file);
    }

    public ImageResponse uploadServiceImage(UUID serviceId, MultipartFile file) {
        return upload(ImageFolder.SERVICE_IMAGE, serviceId, file);
    }

    /**
     * Uploads to {@code folder}. {@code ownerId} is the business or service id; it is ignored for
     * {@link ImageFolder#USER_AVATAR}, which always targets the authenticated user.
     * For single-slot folders the previous image is replaced only after the new one is safely stored.
     */
    public ImageResponse upload(ImageFolder folder, UUID ownerId, MultipartFile file) {
        if (folder == null) {
            throw ImageValidationException.invalidParameter("Geçerli bir klasör seçin.");
        }
        UUID userId = access.currentUserId();
        UUID owner = folder == ImageFolder.USER_AVATAR ? userId : ownerId;
        if (owner == null) {
            throw ImageValidationException.invalidParameter("ownerId zorunludur.");
        }
        access.checkCanUpload(folder, owner);
        rateLimiter.acquire(userId);
        ImageFileValidator.ValidatedImage image = validator.validate(file);

        StoredImage stored = storage.upload(new ImageUpload(image.content(), image.contentType(),
                folder.directory(properties.getRootFolder(), owner), UUID.randomUUID().toString(),
                folder.maxWidth()));

        Persisted persisted;
        try {
            persisted = transactionTemplate.execute(status -> persist(folder, owner, userId, stored));
        } catch (RuntimeException e) {
            log.error("Image metadata could not be saved; removing uploaded asset publicId={} ownerId={}",
                    stored.reference().key(), owner);
            discardQuietly(stored.reference());
            throw e;
        }

        persisted.replaced().forEach(this::removeReplaced);
        log.info("Image uploaded imageId={} folder={} ownerId={} userId={} bytes={}",
                persisted.image().getId(), folder, owner, userId, stored.bytes());
        return toResponse(persisted.image());
    }

    public List<ImageResponse> listBusinessImages(UUID businessId, ImageFolder folder) {
        if (!businessRepository.existsById(businessId)) {
            throw new ResourceNotFoundException("Business not found: " + businessId);
        }
        List<Image> images;
        if (folder == null) {
            images = imageRepository.findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(ImageOwnerType.BUSINESS, businessId);
        } else {
            requireBusinessFolder(folder);
            images = imageRepository.findByOwnerTypeAndOwnerIdAndFolderOrderByCreatedAtDesc(
                    ImageOwnerType.BUSINESS, businessId, folder);
        }
        return images.stream().map(this::toResponse).toList();
    }

    public void deleteBusinessImage(UUID businessId, UUID imageId) {
        Image image = findImage(imageId);
        if (image.getOwnerType() != ImageOwnerType.BUSINESS || !image.getOwnerId().equals(businessId)) {
            throw new ResourceNotFoundException("Image not found: " + imageId);
        }
        delete(image);
    }

    public void delete(UUID imageId) {
        delete(findImage(imageId));
    }

    /**
     * Authorize → delete from storage → delete the row. If storage deletion fails the row is kept
     * (and a {@link StorageException} propagates), so nothing is left pointing at a lost file.
     */
    private void delete(Image image) {
        access.checkCanDelete(image);
        storage.delete(reference(image));
        transactionTemplate.executeWithoutResult(status -> {
            if (image.getFolder().single()) {
                linker.unlinkIfCurrent(image.getFolder(), image.getOwnerId(), image.getUrl());
            }
            imageRepository.deleteById(image.getId());
        });
        log.info("Image deleted imageId={} folder={} ownerId={} userId={}",
                image.getId(), image.getFolder(), image.getOwnerId(), access.currentUserId());
    }

    /**
     * KVKK: removes a user's own images (avatars) from storage and the database.
     * Business and gallery images belong to the business; their {@code uploaded_by} is cleared by the
     * foreign key when the user row is deleted.
     * TODO: Implement user data deletion cascade (call this from the account deletion flow once it exists).
     */
    public void deleteAllForUser(UUID userId) {
        for (Image image : imageRepository.findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(ImageOwnerType.USER, userId)) {
            storage.delete(reference(image));
            imageRepository.delete(image);
            log.info("User image deleted for account removal imageId={} userId={}", image.getId(), userId);
        }
    }

    /** Resized delivery URL; provider URL rules stay in the storage implementation. */
    public String getOptimizedImageUrl(Image image, int width) {
        try {
            String url = storage.getOptimizedUrl(reference(image), width);
            return url != null ? url : image.getUrl();
        } catch (StorageException e) {
            return image.getUrl();
        }
    }

    private Persisted persist(ImageFolder folder, UUID owner, UUID userId, StoredImage stored) {
        Image image = imageRepository.save(Image.builder()
                .storageProvider(stored.reference().provider())
                .publicId(stored.reference().key())
                .url(stored.url())
                .resourceType(stored.resourceType() != null ? stored.resourceType() : "image")
                .folder(folder)
                .ownerType(folder.ownerType())
                .ownerId(owner)
                .uploadedBy(userId)
                .width(stored.width())
                .height(stored.height())
                .format(stored.format())
                .bytes(stored.bytes())
                .build());
        List<Image> replaced = List.of();
        if (folder.single()) {
            linker.link(folder, owner, image.getUrl());
            replaced = imageRepository.findByOwnerTypeAndOwnerIdAndFolderAndIdNot(
                    folder.ownerType(), owner, folder, image.getId());
        }
        return new Persisted(image, replaced);
    }

    /**
     * The new image is already live; failures here only leave an unused asset behind, which is
     * retried on the next replacement because the row is kept.
     * TODO: Add a scheduled cleanup job for replaced images whose storage deletion failed.
     */
    private void removeReplaced(Image old) {
        try {
            storage.delete(reference(old));
            imageRepository.deleteById(old.getId());
        } catch (RuntimeException e) {
            log.warn("Replaced image could not be removed yet imageId={} publicId={} error={}",
                    old.getId(), old.getPublicId(), e.getClass().getSimpleName());
        }
    }

    private void discardQuietly(StorageReference reference) {
        try {
            storage.delete(reference);
        } catch (RuntimeException e) {
            log.warn("Orphaned asset could not be removed publicId={} error={}",
                    reference.key(), e.getClass().getSimpleName());
        }
    }

    private Image findImage(UUID imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + imageId));
    }

    private static void requireBusinessFolder(ImageFolder folder) {
        if (folder == null || folder.ownerType() != ImageOwnerType.BUSINESS) {
            throw ImageValidationException.invalidParameter("Bu klasör işletme fotoğrafları için kullanılamaz.");
        }
    }

    private static StorageReference reference(Image image) {
        return new StorageReference(image.getStorageProvider(), image.getPublicId());
    }

    private ImageResponse toResponse(Image image) {
        return ImageResponse.builder()
                .id(image.getId())
                .url(image.getUrl())
                .thumbnailUrl(getOptimizedImageUrl(image, THUMBNAIL_WIDTH))
                .publicId(image.getPublicId())
                .folder(image.getFolder())
                .ownerType(image.getOwnerType())
                .ownerId(image.getOwnerId())
                .width(image.getWidth())
                .height(image.getHeight())
                .format(image.getFormat())
                .bytes(image.getBytes())
                .createdAt(image.getCreatedAt())
                .build();
    }

    private record Persisted(Image image, List<Image> replaced) {
    }
}
