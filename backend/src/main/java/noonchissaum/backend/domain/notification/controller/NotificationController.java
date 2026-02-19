package noonchissaum.backend.domain.notification.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.notification.dto.res.NotificationRes;
import noonchissaum.backend.domain.notification.service.NotificationService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


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
    public ResponseEntity<ApiResponse<List<NotificationRes>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        List<NotificationRes> notifications = notificationService.findAllByUserId(userId);
        return ResponseEntity.ok(new ApiResponse<>("Notification list retrieved", notifications));
    }

    /**
     * 알림 단건 조회
     */
    @GetMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<NotificationRes>> getNotification(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long notificationId) {
        Long userId = userPrincipal.getUserId();
        NotificationRes response = notificationService.findById(userId, notificationId);
        return ResponseEntity.ok(new ApiResponse<>("Notification detail retrieved", response));
    }
    /**
     *  알림 단건 읽음 처리
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationRes>> readNotification(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long notificationId) {

        Long userId = userPrincipal.getUserId();
        NotificationRes response =
                notificationService.markAsRead(userId, notificationId);

        return ResponseEntity.ok(
                new ApiResponse<>("Notification read", response)
        );
    }
    /**
     *  알림 전체 읽음 처리
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> readAllNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ){
        Long userId = userPrincipal.getUserId();
        int updateCount = notificationService.markAllAsRead(userId);

        return ResponseEntity.ok(
                new ApiResponse<>("All notifications read",updateCount)
        );

    }
    /**
     *  미읽음 알림 개수
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
        @AuthenticationPrincipal UserPrincipal userPrincipal
    ){
        Long userId = userPrincipal.getUserId();
        long count = notificationService.countUnread(userId);

        return ResponseEntity.ok(new ApiResponse<>("Unread notification count", count));
    }
}