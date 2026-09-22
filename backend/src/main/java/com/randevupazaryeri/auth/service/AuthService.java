package com.randevupazaryeri.auth.service;

import com.randevupazaryeri.auth.dto.*;
import com.randevupazaryeri.auth.entity.RefreshToken;
import com.randevupazaryeri.auth.repository.RefreshTokenRepository;
import com.randevupazaryeri.auth.security.JwtService;
import com.randevupazaryeri.common.exception.BusinessRuleException;
import com.randevupazaryeri.common.exception.UnauthorizedException;
import com.randevupazaryeri.common.security.SecurityUtils;
import com.randevupazaryeri.config.JwtProperties;
import com.randevupazaryeri.user.dto.UserResponse;
import com.randevupazaryeri.user.entity.Role;
import com.randevupazaryeri.user.entity.User;
import com.randevupazaryeri.user.mapper.UserMapper;
import com.randevupazaryeri.user.repository.UserRepository;
import com.randevupazaryeri.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() != Role.CUSTOMER && request.getRole() != Role.PROVIDER) {
            throw new BusinessRuleException("Only CUSTOMER or PROVIDER roles can register");
        }
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BusinessRuleException("Email already registered");
        }
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().trim().toLowerCase())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .isActive(true)
                .build();
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().trim().toLowerCase(), request.getPassword()));
        User user = userService.getByEmail(request.getEmail().trim().toLowerCase());
        if (!user.isActive()) {
            throw new UnauthorizedException("Account is inactive");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String hash = hashToken(request.getRefreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            stored.setRevoked(true);
            throw new UnauthorizedException("Refresh token expired");
        }
        stored.setRevoked(true);
        User user = stored.getUser();
        return issueTokens(user);
    }

    @Transactional
    public void logout() {
        UUID userId = SecurityUtils.currentUserId();
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public EmailLookupResponse lookupEmail(EmailLookupRequest request) {
        boolean exists = userRepository.existsByEmailIgnoreCase(request.getEmail().trim().toLowerCase());
        return EmailLookupResponse.builder().exists(exists).build();
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        return UserMapper.toResponse(userService.getById(SecurityUtils.currentUserId()));
    }

    private AuthResponse issueTokens(User user) {
        String access = jwtService.createAccessToken(user.getId(), user.getRole());
        String rawRefresh = generateRawRefreshToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawRefresh))
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return AuthResponse.builder()
                .accessToken(access)
                .refreshToken(rawRefresh)
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .user(UserMapper.toResponse(user))
                .build();
    }

    private String generateRawRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash refresh token", e);
        }
    }
}
