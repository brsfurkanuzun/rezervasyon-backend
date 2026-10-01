package com.randevupazaryeri.push.service;

import com.randevupazaryeri.common.security.SecurityUtils;
import com.randevupazaryeri.push.dto.RegisterDeviceRequest;
import com.randevupazaryeri.push.entity.DeviceToken;
import com.randevupazaryeri.push.repository.DeviceTokenRepository;
import com.randevupazaryeri.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {
    private final DeviceTokenRepository deviceTokenRepository;
    private final UserService userService;

    /** A device token belongs to whoever signed in on that device last. */
    @Transactional
    public void register(RegisterDeviceRequest request) {
        String token = request.getToken().toLowerCase(Locale.ROOT);
        DeviceToken device = deviceTokenRepository.findByToken(token)
                .orElseGet(() -> DeviceToken.builder().token(token).build());
        device.setUser(userService.getById(SecurityUtils.currentUserId()));
        device.setApp(request.getApp());
        device.setEnvironment(request.getEnvironment());
        deviceTokenRepository.save(device);
    }

    @Transactional
    public void unregister(String token) {
        deviceTokenRepository.deleteByTokenAndUserId(token.toLowerCase(Locale.ROOT), SecurityUtils.currentUserId());
    }
}
