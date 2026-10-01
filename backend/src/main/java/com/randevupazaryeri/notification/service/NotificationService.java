package com.randevupazaryeri.notification.service;

import com.randevupazaryeri.common.exception.ResourceNotFoundException;
import com.randevupazaryeri.common.security.SecurityUtils;
import com.randevupazaryeri.notification.dto.NotificationResponse;
import com.randevupazaryeri.notification.entity.Notification;
import com.randevupazaryeri.notification.repository.NotificationRepository;
import com.randevupazaryeri.push.entity.PushApp;
import com.randevupazaryeri.push.service.PushNotificationEvent;
import com.randevupazaryeri.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * In-app notification store, optionally fanned out as an iOS push to one of the apps.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    /** Stores the notification and pushes it to the user's devices for {@code pushApp}. */
    @Transactional
    public void notifyUser(UUID userId, String type, String title, String message,
                           PushApp pushApp, Map<String, String> pushData) {
        notifyUser(userId, type, title, message);
        eventPublisher.publishEvent(new PushNotificationEvent(userId, pushApp, title, message, pushData));
    }

    @Transactional
    public void notifyUser(UUID userId, String type, String title, String message) {
        Notification n = Notification.builder()
                .user(userService.getById(userId))
                .type(type)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> myNotifications(Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(SecurityUtils.currentUserId(), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public NotificationResponse markRead(UUID id) {
        Notification n = notificationRepository.findByIdAndUserId(id, SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        n.setRead(true);
        return toResponse(n);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId()).type(n.getType()).title(n.getTitle()).message(n.getMessage())
                .isRead(n.isRead()).createdAt(n.getCreatedAt()).build();
    }
}
