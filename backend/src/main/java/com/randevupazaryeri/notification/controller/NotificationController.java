package com.randevupazaryeri.notification.controller;

import com.randevupazaryeri.common.dto.ApiResponse;
import com.randevupazaryeri.notification.dto.NotificationResponse;
import com.randevupazaryeri.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List my notifications")
    public ApiResponse<List<NotificationResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
        Page<NotificationResponse> page = notificationService.myNotifications(pageable);
        return ApiResponse.ofPage(page);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable UUID id) {
        return ApiResponse.ok(notificationService.markRead(id));
    }
}
