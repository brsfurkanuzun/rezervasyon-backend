package com.randevupazaryeri.push.service;

import com.randevupazaryeri.push.entity.PushApp;

import java.util.Map;
import java.util.UUID;

public record PushNotificationEvent(UUID userId, PushApp app, String title, String body, Map<String, String> data) {
}
