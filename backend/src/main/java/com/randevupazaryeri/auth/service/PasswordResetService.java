package com.randevupazaryeri.auth.service;

import com.randevupazaryeri.auth.dto.AuthResponse;
import com.randevupazaryeri.auth.dto.ForgotPasswordRequest;
import com.randevupazaryeri.auth.dto.ResetPasswordRequest;
import com.randevupazaryeri.auth.entity.PasswordResetCode;
import com.randevupazaryeri.auth.repository.PasswordResetCodeRepository;
import com.randevupazaryeri.auth.repository.RefreshTokenRepository;
import com.randevupazaryeri.common.exception.BusinessRuleException;
import com.randevupazaryeri.mail.EmailService;
import com.randevupazaryeri.user.entity.User;
import com.randevupazaryeri.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * "Forgot password" with a six-digit emailed code. Requests never reveal whether the address has an
 * account. A code is valid for 15 minutes and five tries; at most five codes are sent per hour, and a new
 * code waits a minute after the previous one.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    static final Duration CODE_TTL = Duration.ofMinutes(15);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final int MAX_ATTEMPTS = 5;
    static final int MAX_CODES_PER_HOUR = 5;
    static final String INVALID_CODE = "Invalid or expired reset code";

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository codeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthService authService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void requestCode(ForgotPasswordRequest request) {
        Optional<User> found = findActive(request.getEmail()).filter(user -> canReceiveEmail(user.getEmail()));
        if (found.isEmpty()) {
            return;
        }
        User user = found.get();
        Instant now = Instant.now();
        codeRepository.deleteOlderThan(user.getId(), now.minus(Duration.ofHours(1)));
        boolean coolingDown = codeRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                .filter(latest -> latest.getCreatedAt().plus(RESEND_COOLDOWN).isAfter(now))
                .isPresent();
        if (coolingDown || codeRepository.countByUserIdAndCreatedAtAfter(user.getId(), now.minus(Duration.ofHours(1)))
                >= MAX_CODES_PER_HOUR) {
            return;
        }
        String code = "%06d".formatted(secureRandom.nextInt(1_000_000));
        codeRepository.save(PasswordResetCode.builder()
                .userId(user.getId())
                .codeHash(passwordEncoder.encode(code))
                .attempts(0)
                .expiresAt(now.plus(CODE_TTL))
                .createdAt(now)
                .build());
        emailService.send(user.getEmail(), "resplz şifre sıfırlama kodun", emailBody(user, code));
    }

    /** Wrong codes still count as attempts, so the failure must not roll the counter back. */
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public AuthResponse reset(ResetPasswordRequest request) {
        User user = findActive(request.getEmail()).orElseThrow(() -> new BusinessRuleException(INVALID_CODE));
        PasswordResetCode latest = codeRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                .filter(code -> code.getExpiresAt().isAfter(Instant.now()) && code.getAttempts() < MAX_ATTEMPTS)
                .orElseThrow(() -> new BusinessRuleException(INVALID_CODE));
        if (!passwordEncoder.matches(request.getCode(), latest.getCodeHash())) {
            latest.setAttempts(latest.getAttempts() + 1);
            codeRepository.save(latest);
            throw new BusinessRuleException(INVALID_CODE);
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordSet(true);
        // Flushed first: the bulk updates below clear the persistence context.
        userRepository.saveAndFlush(user);
        codeRepository.deleteByUserId(user.getId());
        refreshTokenRepository.revokeAllByUserId(user.getId());
        return authService.issueTokens(user);
    }

    private Optional<User> findActive(String email) {
        return userRepository.findByEmailIgnoreCase(email.trim().toLowerCase()).filter(User::isActive);
    }

    /** Placeholder addresses created for Apple sign-ins without a shared email cannot receive mail. */
    private boolean canReceiveEmail(String email) {
        return !email.endsWith("@appleid.rezplz.app") && !email.endsWith(".invalid");
    }

    private String emailBody(User user, String code) {
        return """
                Merhaba %s,

                resplz hesabının şifresini sıfırlamak için kodun:

                %s

                Kod 15 dakika geçerli. Bu isteği sen yapmadıysan bu e-postayı dikkate alma; şifren değişmez.

                resplz
                """.formatted(user.getFirstName(), code);
    }
}
