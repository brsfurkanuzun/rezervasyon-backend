package com.randevupazaryeri.auth.service;

import com.randevupazaryeri.auth.dto.AuthResponse;
import com.randevupazaryeri.auth.dto.ForgotPasswordRequest;
import com.randevupazaryeri.auth.dto.ResetPasswordRequest;
import com.randevupazaryeri.auth.entity.PasswordResetToken;
import com.randevupazaryeri.auth.repository.PasswordResetTokenRepository;
import com.randevupazaryeri.auth.repository.RefreshTokenRepository;
import com.randevupazaryeri.common.exception.BusinessRuleException;
import com.randevupazaryeri.config.PasswordResetProperties;
import com.randevupazaryeri.mail.EmailService;
import com.randevupazaryeri.user.entity.Role;
import com.randevupazaryeri.user.entity.User;
import com.randevupazaryeri.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * "Forgot password" with an emailed one-time link. Requests never reveal whether the address has an
 * account. A link is valid for 30 minutes and works once; at most five links are sent per hour, and a new
 * one waits a minute after the previous one. Business accounts get a link to the partner site.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final int MAX_LINKS_PER_HOUR = 5;
    static final String INVALID_LINK = "Invalid or expired reset link";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthService authService;
    private final PasswordResetProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void requestLink(ForgotPasswordRequest request) {
        Optional<User> found = userRepository.findByEmailIgnoreCase(request.getEmail().trim().toLowerCase())
                .filter(User::isActive)
                .filter(user -> canReceiveEmail(user.getEmail()));
        if (found.isEmpty()) {
            return;
        }
        User user = found.get();
        Instant now = Instant.now();
        tokenRepository.deleteOlderThan(user.getId(), now.minus(Duration.ofHours(1)));
        boolean coolingDown = tokenRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                .filter(latest -> latest.getCreatedAt().plus(RESEND_COOLDOWN).isAfter(now))
                .isPresent();
        if (coolingDown || tokenRepository.countByUserIdAndCreatedAtAfter(user.getId(), now.minus(Duration.ofHours(1)))
                >= MAX_LINKS_PER_HOUR) {
            return;
        }
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        tokenRepository.save(PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(hash(token))
                .expiresAt(now.plus(TOKEN_TTL))
                .createdAt(now)
                .build());
        String page = user.getRole() == Role.PROVIDER ? properties.getPartnerUrl() : properties.getCustomerUrl();
        emailService.send(user.getEmail(), "resplz şifre sıfırlama bağlantın", emailBody(user, page + "#token=" + token));
    }

    @Transactional
    public AuthResponse reset(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(request.getToken().trim()))
                .filter(found -> found.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new BusinessRuleException(INVALID_LINK));
        User user = userRepository.findById(token.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new BusinessRuleException(INVALID_LINK));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordSet(true);
        // Flushed first: the bulk updates below clear the persistence context.
        userRepository.saveAndFlush(user);
        tokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.revokeAllByUserId(user.getId());
        return authService.issueTokens(user);
    }

    /** Placeholder addresses created for Apple sign-ins without a shared email cannot receive mail. */
    private boolean canReceiveEmail(String email) {
        return !email.endsWith("@appleid.rezplz.app") && !email.endsWith(".invalid");
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String emailBody(User user, String link) {
        return """
                Merhaba %s,

                resplz hesabının şifresini sıfırlamak için bu bağlantıyı aç:

                %s

                Bağlantı 30 dakika geçerli ve yalnızca bir kez kullanılabilir. Bu isteği sen yapmadıysan bu e-postayı dikkate alma; şifren değişmez.

                resplz
                """.formatted(user.getFirstName(), link);
    }
}
