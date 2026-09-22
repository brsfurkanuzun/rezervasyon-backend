package com.randevupazaryeri.notification.service;

import com.randevupazaryeri.common.exception.ResourceNotFoundException;
import com.randevupazaryeri.common.security.SecurityUtils;
import com.randevupazaryeri.notification.dto.NotificationResponse;
import com.randevupazaryeri.notification.entity.Notification;
import com.randevupazaryeri.notification.repository.NotificationRepository;
import com.randevupazaryeri.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * In-app notification store. Channel adapters (email/SMS/push/WhatsApp) can plug in later.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserService userService;

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
