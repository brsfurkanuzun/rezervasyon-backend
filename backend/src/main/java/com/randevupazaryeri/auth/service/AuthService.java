package com.randevupazaryeri.auth.service;

import com.randevupazaryeri.auth.dto.*;
import com.randevupazaryeri.auth.entity.RefreshToken;
import com.randevupazaryeri.auth.repository.RefreshTokenRepository;
import com.randevupazaryeri.auth.security.JwtService;
import com.randevupazaryeri.auth.security.AppleIdentityVerifier;
import com.randevupazaryeri.auth.security.GoogleIdentityVerifier;
import com.randevupazaryeri.config.AppleProperties;
import com.randevupazaryeri.config.GoogleProperties;
import com.randevupazaryeri.common.exception.BusinessRuleException;
import com.randevupazaryeri.common.exception.UnauthorizedException;
import com.randevupazaryeri.common.security.SecurityUtils;
import com.randevupazaryeri.consent.service.ConsentService;
import com.randevupazaryeri.config.JwtProperties;
import com.randevupazaryeri.employee.repository.EmployeeRepository;
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
import java.util.Objects;
import java.util.Optional;
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
    private final EmployeeRepository employeeRepository;
    private final AppleIdentityVerifier appleIdentityVerifier;
    private final AppleProperties appleProperties;
    private final GoogleIdentityVerifier googleIdentityVerifier;
    private final GoogleProperties googleProperties;
    private final ConsentService consentService;
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
        consentService.recordSignUp(user, request.getConsents());
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
        // V26 flagged every account with a social link, including ones that also had a real password.
        if (!user.isPasswordSet()) {
            user.setPasswordSet(true);
            userRepository.save(user);
        }
        return issueTokens(user);
    }

    /**
     * Signs in with an Apple identity token. Existing accounts are matched by Apple id, then by
     * Apple-verified email (linking the two); otherwise a new account is created with {@code role}.
     */
    @Transactional
    public AuthResponse loginWithApple(AppleLoginRequest request) {
        Role role = request.getRole() == null ? Role.CUSTOMER : request.getRole();
        if (role != Role.CUSTOMER && role != Role.PROVIDER) {
            throw new BusinessRuleException("Only CUSTOMER or PROVIDER roles can register");
        }
        AppleIdentityVerifier.AppleIdentity identity = appleIdentityVerifier.verify(request.getIdentityToken());
        User user = userRepository.findByAppleUserId(identity.subject())
                .or(() -> identity.email() != null && identity.emailVerified()
                        ? userRepository.findByEmailIgnoreCase(identity.email())
                        : Optional.empty())
                .orElseGet(() -> newAppleUser(identity, request, role));
        if (!user.isActive()) {
            throw new UnauthorizedException("Account is inactive");
        }
        boolean created = user.getId() == null;
        user.setAppleUserId(identity.subject());
        userRepository.save(user);
        if (created) {
            consentService.recordSignUp(user, request.getConsents());
        }
        return issueTokens(user);
    }

    private User newAppleUser(AppleIdentityVerifier.AppleIdentity identity, AppleLoginRequest request, Role role) {
        String email = identity.email() != null
                ? identity.email().trim().toLowerCase()
                : identity.subject().replaceAll("[^A-Za-z0-9]", "").toLowerCase() + "@appleid.rezplz.app";
        String firstName = blankToNull(request.getFirstName());
        String lastName = blankToNull(request.getLastName());
        return User.builder()
                .firstName(firstName != null ? firstName : "rezplz")
                .lastName(lastName != null ? lastName : "Kullanıcısı")
                .email(email)
                .passwordHash(passwordEncoder.encode(generateRawRefreshToken()))
                .passwordSet(false)
                .role(role)
                .isActive(true)
                .build();
    }

    /**
     * Signs in with a Google ID token. Existing accounts are matched by Google id, then by email when
     * Google is authoritative for it (linking the two); otherwise a new account is created with {@code role}.
     */
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        Role role = request.getRole() == null ? Role.CUSTOMER : request.getRole();
        if (role != Role.CUSTOMER && role != Role.PROVIDER) {
            throw new BusinessRuleException("Only CUSTOMER or PROVIDER roles can register");
        }
        GoogleIdentityVerifier.GoogleIdentity identity = googleIdentityVerifier.verify(request.getIdToken());
        if (identity.email() == null || !identity.emailVerified()) {
            throw new UnauthorizedException("Google account email is not verified");
        }
        String email = identity.email().trim().toLowerCase();
        User user = userRepository.findByGoogleUserId(identity.subject())
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(email)
                        .map(existing -> {
                            if (!identity.emailIsAuthoritative()) {
                                throw new BusinessRuleException(
                                        "An account with this email already exists. Sign in with your password.");
                            }
                            return existing;
                        })
                        .orElseGet(() -> newGoogleUser(identity, email, role)));
        if (!user.isActive()) {
            throw new UnauthorizedException("Account is inactive");
        }
        boolean created = user.getId() == null;
        user.setGoogleUserId(identity.subject());
        userRepository.save(user);
        if (created) {
            consentService.recordSignUp(user, request.getConsents());
        }
        return issueTokens(user);
    }

    public AppleConfigResponse appleConfig() {
        String serviceId = appleProperties.getWebServiceId();
        return AppleConfigResponse.builder()
                .clientId(serviceId == null || serviceId.isBlank() ? null : serviceId.trim())
                .build();
    }

    public GoogleConfigResponse googleConfig() {
        String clientId = googleProperties.getWebClientId();
        return GoogleConfigResponse.builder()
                .clientId(clientId == null || clientId.isBlank() ? null : clientId.trim())
                .build();
    }

    private User newGoogleUser(GoogleIdentityVerifier.GoogleIdentity identity, String email, Role role) {
        String firstName = truncate(blankToNull(identity.firstName()), 100);
        String lastName = truncate(blankToNull(identity.lastName()), 100);
        return User.builder()
                .firstName(firstName != null ? firstName : "ResPlz")
                .lastName(lastName != null ? lastName : "Kullanıcısı")
                .email(email)
                .passwordHash(passwordEncoder.encode(generateRawRefreshToken()))
                .passwordSet(false)
                .role(role)
                .isActive(true)
                .build();
    }

    /**
     * Sets a new password. Accounts that never chose one (social sign-ups) skip the current-password
     * check. Other sessions are signed out; the caller gets fresh tokens.
     */
    @Transactional
    public AuthResponse changePassword(ChangePasswordRequest request) {
        User user = userService.getById(SecurityUtils.currentUserId());
        if (user.isPasswordSet()) {
            String current = request.getCurrentPassword();
            if (current == null || !passwordEncoder.matches(current, user.getPasswordHash())) {
                throw new BusinessRuleException("Current password is incorrect");
            }
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordSet(true);
        userRepository.saveAndFlush(user);
        refreshTokenRepository.revokeAllByUserId(user.getId());
        return issueTokens(user);
    }

    @Transactional
    public UserResponse linkGoogle(GoogleLinkRequest request) {
        User user = userService.getById(SecurityUtils.currentUserId());
        String subject = googleIdentityVerifier.verify(request.getIdToken()).subject();
        userRepository.findByGoogleUserId(subject)
                .filter(owner -> !owner.getId().equals(user.getId()))
                .ifPresent(owner -> {
                    throw new BusinessRuleException("This Google account is already linked to another account");
                });
        user.setGoogleUserId(subject);
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse unlinkGoogle() {
        User user = userService.getById(SecurityUtils.currentUserId());
        if (user.getGoogleUserId() != null) {
            requireOtherSignInMethod(user.isPasswordSet() || user.getAppleUserId() != null);
            user.setGoogleUserId(null);
            userRepository.save(user);
        }
        return UserMapper.toResponse(user);
    }

    @Transactional
    public UserResponse linkApple(AppleLinkRequest request) {
        User user = userService.getById(SecurityUtils.currentUserId());
        String subject = appleIdentityVerifier.verify(request.getIdentityToken()).subject();
        userRepository.findByAppleUserId(subject)
                .filter(owner -> !owner.getId().equals(user.getId()))
                .ifPresent(owner -> {
                    throw new BusinessRuleException("This Apple account is already linked to another account");
                });
        user.setAppleUserId(subject);
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse unlinkApple() {
        User user = userService.getById(SecurityUtils.currentUserId());
        if (user.getAppleUserId() != null) {
            requireOtherSignInMethod(user.isPasswordSet() || user.getGoogleUserId() != null);
            user.setAppleUserId(null);
            userRepository.save(user);
        }
        return UserMapper.toResponse(user);
    }

    private void requireOtherSignInMethod(boolean hasOther) {
        if (!hasOther) {
            throw new BusinessRuleException("This is your only way to sign in. Set a password before unlinking it.");
        }
    }

    /**
     * Turns a customer account into a business account. Business accounts keep every customer
     * capability, so the same login works in both apps. Tokens are reissued because they carry the role.
     */
    @Transactional
    public AuthResponse upgradeToProvider() {
        User user = userService.getById(SecurityUtils.currentUserId());
        if (user.getRole() == Role.CUSTOMER) {
            user.setRole(Role.PROVIDER);
            userRepository.save(user);
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

    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = userService.getById(SecurityUtils.currentUserId());
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhone(blankToNull(request.getPhone()));
        user.setBirthDate(request.getBirthDate());
        user.setGender(blankToNull(request.getGender()));
        String previousPhoto = user.getPhotoUrl();
        user.setPhotoUrl(blankToNull(request.getPhotoUrl()));
        if (!Objects.equals(previousPhoto, user.getPhotoUrl())) {
            employeeRepository.followAccountPhoto(user.getId(), previousPhoto, user.getPhotoUrl());
        }
        return UserMapper.toResponse(userRepository.save(user));
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

    private String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
