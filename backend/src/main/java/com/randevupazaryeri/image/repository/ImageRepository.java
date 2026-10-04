package com.randevupazaryeri.image.repository;

import com.randevupazaryeri.image.entity.Image;
import com.randevupazaryeri.image.entity.ImageFolder;
import com.randevupazaryeri.image.entity.ImageOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ImageRepository extends JpaRepository<Image, UUID> {

    List<Image> findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(ImageOwnerType ownerType, UUID ownerId);

    List<Image> findByOwnerTypeAndOwnerIdAndFolderOrderByCreatedAtDesc(ImageOwnerType ownerType, UUID ownerId,
                                                                       ImageFolder folder);

    List<Image> findByOwnerTypeAndOwnerIdAndFolderAndIdNot(ImageOwnerType ownerType, UUID ownerId,
                                                           ImageFolder folder, UUID id);

    List<Image> findByUploadedBy(UUID uploadedBy);
}
