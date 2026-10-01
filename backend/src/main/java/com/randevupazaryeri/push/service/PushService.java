package com.randevupazaryeri.push.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.randevupazaryeri.push.entity.DeviceToken;
import com.randevupazaryeri.push.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Delivers push notifications once the triggering transaction has committed, off the request thread. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {
    private final DeviceTokenRepository deviceTokenRepository;
    private final ApnsClient apnsClient;
    private final ObjectMapper objectMapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPushNotification(PushNotificationEvent event) {
        if (!apnsClient.isConfigured(event.app())) {
            return;
        }
        List<DeviceToken> devices = deviceTokenRepository.findByUserIdAndApp(event.userId(), event.app());
        if (devices.isEmpty()) {
            return;
        }
        String payload;
        try {
            payload = objectMapper.writeValueAsString(payload(event));
        } catch (JsonProcessingException ex) {
            log.warn("Could not serialize push payload: {}", ex.getMessage());
            return;
        }
        for (DeviceToken device : devices) {
            ApnsClient.Result result = apnsClient.send(device.getToken(), device.getApp(), device.getEnvironment(), payload);
            if (result == ApnsClient.Result.INVALID_TOKEN) {
                deviceTokenRepository.deleteByToken(device.getToken());
            }
        }
    }

    private Map<String, Object> payload(PushNotificationEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("aps", Map.of(
                "alert", Map.of("title", event.title(), "body", event.body()),
                "sound", "default"));
        if (event.data() != null) {
            payload.putAll(event.data());
        }
        return payload;
    }
}
