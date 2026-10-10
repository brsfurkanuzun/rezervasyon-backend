package com.randevupazaryeri.auth.service;

import com.randevupazaryeri.appointment.service.AppointmentService;
import com.randevupazaryeri.auth.dto.DeleteAccountRequest;
import com.randevupazaryeri.auth.security.AppleTokenRevoker;
import com.randevupazaryeri.business.entity.Business;
import com.randevupazaryeri.business.repository.BusinessRepository;
import com.randevupazaryeri.business.service.BusinessDeletionService;
import com.randevupazaryeri.common.exception.BusinessRuleException;
import com.randevupazaryeri.common.security.SecurityUtils;
import com.randevupazaryeri.employee.entity.Employee;
import com.randevupazaryeri.employee.repository.EmployeeRepository;
import com.randevupazaryeri.image.service.ImageService;
import com.randevupazaryeri.user.entity.User;
import com.randevupazaryeri.user.service.UserService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * Deletes the signed-in account. Personal data, sessions, addresses, favorites, notifications, device
 * tokens, consents and the avatar are removed. The user row stays anonymised so businesses keep their
 * appointment history and reviews lose the name. Open appointments are cancelled and the business told.
 * Businesses the user owns are deleted with everything that belongs to them.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    static final String DELETED_FIRST_NAME = "Silinmiş";
    static final String DELETED_LAST_NAME = "Kullanıcı";

    private final UserService userService;
    private final BusinessRepository businessRepository;
    private final BusinessDeletionService businessDeletionService;
    private final EmployeeRepository employeeRepository;
    private final AppointmentService appointmentService;
    private final ImageService imageService;
    private final AppleTokenRevoker appleTokenRevoker;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbc;
    private final SecureRandom secureRandom = new SecureRandom();

    public void deleteCurrentAccount(DeleteAccountRequest request) {
        UUID userId = SecurityUtils.currentUserId();
        User user = userService.getById(userId);
        if (user.isPasswordSet()) {
            String password = request == null ? null : request.getPassword();
            if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
                throw new BusinessRuleException("Current password is incorrect");
            }
        }
        for (Business business : businessRepository.findByOwnerId(userId)) {
            businessDeletionService.delete(business.getId());
        }

        // Storage first: if it fails the account is left untouched and the user can retry.
        imageService.deleteAllForUser(userId);

        String accountPhoto = user.getPhotoUrl();
        String appleUserId = user.getAppleUserId();
        transactionTemplate.executeWithoutResult(status -> {
            appointmentService.cancelOpenForDeletedCustomer(userId);
            for (Employee employee : employeeRepository.findByUserId(userId)) {
                if (accountPhoto != null && Objects.equals(employee.getPhotoUrl(), accountPhoto)) {
                    employee.setPhotoUrl(null);
                }
                employee.setUser(null);
            }
            entityManager.flush();

            jdbc.update("DELETE FROM refresh_tokens WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM device_tokens WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM user_addresses WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM favorites WHERE customer_id = ?", userId);
            jdbc.update("DELETE FROM notifications WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM user_notice_receipts WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM user_consents WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM password_reset_tokens WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM review_reports WHERE reporter_id = ?", userId);
            jdbc.update("DELETE FROM user_blocks WHERE blocker_id = ?", userId);
            jdbc.update("UPDATE appointments SET customer_note = NULL WHERE customer_id = ?", userId);
            jdbc.update("""
                    UPDATE users SET first_name = ?, last_name = ?, email = ?, phone = NULL, photo_url = NULL,
                        birth_date = NULL, gender = NULL, apple_user_id = NULL, google_user_id = NULL,
                        password_hash = ?, password_set = FALSE, is_active = FALSE,
                        deleted_at = ?, updated_at = ?
                    WHERE id = ?
                    """,
                    DELETED_FIRST_NAME, DELETED_LAST_NAME, "deleted-" + userId + "@deleted.resplz.invalid",
                    passwordEncoder.encode(randomSecret()), Timestamp.from(Instant.now()),
                    Timestamp.from(Instant.now()), userId);
        });
        log.info("Account deleted userId={}", userId);

        if (appleUserId != null && request != null) {
            appleTokenRevoker.revoke(request.getAppleAuthorizationCode(), request.getAppleClientId(), appleUserId);
        }
    }

    private String randomSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
