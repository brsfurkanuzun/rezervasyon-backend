package com.randevupazaryeri.image.service;

import com.randevupazaryeri.business.entity.Business;
import com.randevupazaryeri.common.exception.ResourceNotFoundException;
import com.randevupazaryeri.employee.entity.Employee;
import com.randevupazaryeri.employee.repository.EmployeeRepository;
import com.randevupazaryeri.image.entity.ImageFolder;
import com.randevupazaryeri.serviceoffer.entity.ServiceOffer;
import com.randevupazaryeri.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Keeps the existing URL columns ({@code users.photo_url}, {@code businesses.logo_url},
 * {@code businesses.cover_image_url}, {@code services.image_url}, {@code employees.photo_url}) in sync
 * with single-slot images.
 * Must be called inside a transaction; the owner row is locked so concurrent replacements serialize.
 */
@Component
@RequiredArgsConstructor
public class ImageOwnerLinker {

    private final EntityManager entityManager;
    private final EmployeeRepository employeeRepository;

    public void link(ImageFolder folder, UUID ownerId, String url) {
        switch (folder) {
            case USER_AVATAR -> {
                User user = lock(User.class, ownerId);
                String previous = user.getPhotoUrl();
                user.setPhotoUrl(url);
                employeeRepository.followAccountPhoto(ownerId, previous, url);
            }
            case EMPLOYEE_PHOTO -> lock(Employee.class, ownerId).setPhotoUrl(url);
            case BUSINESS_PROFILE -> lock(Business.class, ownerId).setLogoUrl(url);
            case BUSINESS_COVER -> lock(Business.class, ownerId).setCoverImageUrl(url);
            case SERVICE_IMAGE -> lock(ServiceOffer.class, ownerId).setImageUrl(url);
            case BUSINESS_GALLERY -> {
            }
        }
    }

    /** Clears the owner's URL column only if it still points at {@code url}. */
    public void unlinkIfCurrent(ImageFolder folder, UUID ownerId, String url) {
        switch (folder) {
            case USER_AVATAR -> {
                User user = lockIfPresent(User.class, ownerId);
                if (user != null && Objects.equals(user.getPhotoUrl(), url)) user.setPhotoUrl(null);
                employeeRepository.followAccountPhoto(ownerId, url, null);
            }
            case EMPLOYEE_PHOTO -> {
                Employee employee = lockIfPresent(Employee.class, ownerId);
                if (employee != null && Objects.equals(employee.getPhotoUrl(), url)) employee.setPhotoUrl(null);
            }
            case BUSINESS_PROFILE -> {
                Business business = lockIfPresent(Business.class, ownerId);
                if (business != null && Objects.equals(business.getLogoUrl(), url)) business.setLogoUrl(null);
            }
            case BUSINESS_COVER -> {
                Business business = lockIfPresent(Business.class, ownerId);
                if (business != null && Objects.equals(business.getCoverImageUrl(), url)) business.setCoverImageUrl(null);
            }
            case SERVICE_IMAGE -> {
                ServiceOffer service = lockIfPresent(ServiceOffer.class, ownerId);
                if (service != null && Objects.equals(service.getImageUrl(), url)) service.setImageUrl(null);
            }
            case BUSINESS_GALLERY -> {
            }
        }
    }

    private <T> T lock(Class<T> type, UUID id) {
        T entity = lockIfPresent(type, id);
        if (entity == null) {
            throw new ResourceNotFoundException(type.getSimpleName() + " not found: " + id);
        }
        return entity;
    }

    private <T> T lockIfPresent(Class<T> type, UUID id) {
        return entityManager.find(type, id, LockModeType.PESSIMISTIC_WRITE);
    }
}
