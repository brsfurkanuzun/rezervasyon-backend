package com.randevupazaryeri.image.service;

import com.randevupazaryeri.business.service.BusinessOwnershipService;
import com.randevupazaryeri.common.exception.ForbiddenException;
import com.randevupazaryeri.common.exception.ResourceNotFoundException;
import com.randevupazaryeri.common.security.SecurityUtils;
import com.randevupazaryeri.image.entity.Image;
import com.randevupazaryeri.image.entity.ImageFolder;
import com.randevupazaryeri.serviceoffer.entity.ServiceOffer;
import com.randevupazaryeri.serviceoffer.repository.ServiceOfferRepository;
import com.randevupazaryeri.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Who may upload to or delete from an image owner.
 * <ul>
 *   <li>Business profile/cover and service images: the business owner (or an admin).</li>
 *   <li>Business gallery: the owner or an active team member.</li>
 *   <li>User avatar: only the user themself.</li>
 *   <li>Delete: admin, the owner of the business/user, or the uploader while still a team member.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ImageAccessPolicy {

    private final BusinessOwnershipService ownership;
    private final ServiceOfferRepository serviceOfferRepository;

    public UUID currentUserId() {
        return SecurityUtils.currentUserId();
    }

    public void checkCanUpload(ImageFolder folder, UUID ownerId) {
        switch (folder) {
            case USER_AVATAR -> {
                if (!ownerId.equals(currentUserId())) {
                    throw new ForbiddenException("You can only change your own photo");
                }
            }
            case BUSINESS_PROFILE, BUSINESS_COVER -> ownership.requireOwnedBusiness(ownerId);
            case BUSINESS_GALLERY -> ownership.requireMember(ownerId);
            case SERVICE_IMAGE -> ownership.requireOwnedBusiness(businessOfService(ownerId));
        }
    }

    public void checkCanDelete(Image image) {
        var principal = SecurityUtils.currentPrincipal();
        if (principal.getRole() == Role.ADMIN) {
            return;
        }
        UUID me = principal.getId();
        switch (image.getOwnerType()) {
            case USER -> {
                if (!image.getOwnerId().equals(me)) {
                    throw new ForbiddenException("You cannot delete this image");
                }
            }
            case BUSINESS -> {
                var access = ownership.requireMember(image.getOwnerId());
                if (!access.isOwner() && !me.equals(image.getUploadedBy())) {
                    throw new ForbiddenException("You cannot delete this image");
                }
            }
            case SERVICE -> ownership.requireOwnedBusiness(businessOfService(image.getOwnerId()));
        }
    }

    private UUID businessOfService(UUID serviceId) {
        ServiceOffer service = serviceOfferRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + serviceId));
        return service.getBusiness().getId();
    }
}
