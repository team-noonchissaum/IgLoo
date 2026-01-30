package noonchissaum.backend.domain.notification.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.notification.dto.NotificationResponse;
import noonchissaum.backend.domain.notification.service.NotificationService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        List<NotificationResponse> notifications = notificationService.findAllByUserId(userId);
        return ResponseEntity.ok(new ApiResponse<>("Notification list retrieved", notifications));
    }

    /**
     * 알림 단건 조회
     */
    @GetMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotification(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long notificationId) {
        Long userId = userPrincipal.getUserId();
        NotificationResponse response = notificationService.findById(userId, notificationId);
        return ResponseEntity.ok(new ApiResponse<>("Notification detail retrieved", response));
    }
}