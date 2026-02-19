package noonchissaum.backend.domain.notification.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.notification.dto.res.NotificationRes;
import noonchissaum.backend.domain.notification.entity.Notification;
import noonchissaum.backend.domain.notification.entity.NotificationType;
import noonchissaum.backend.domain.notification.repository.NotificationRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 알림 생성
     */
    @Transactional
    public Notification create(Long userId, NotificationType type, String message, String refType, Long refId){
        User user = userService.getUserByUserId(userId);

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .message(message)
                .refType(refType)
                .refId(refId)
                .build();
        return notificationRepository.save(notification);
    }

    /**
     * 알림 생성 + WS 푸시
     */
    @Transactional
    public NotificationRes sendNotification(Long userId, NotificationType type, String message, String refType, Long refId) {
        Notification saved = create(userId, type, message, refType, refId);
        NotificationRes payload = NotificationRes.from(saved);

        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/notifications",
                payload
        );

        return payload;
    }

    /**
     * 알람 전체 리스트
     */
    @Transactional(readOnly = true)
    public List<NotificationRes> findAllByUserId(Long userId) {
        return notificationRepository.findAllByUserId(userId).stream()
                .map(NotificationRes::from)
                .collect(Collectors.toList());
    }

    /**
     * 알림 단건 조회 (본인 확인 포함)
     * */
    @Transactional(readOnly = true)
    public NotificationRes findById(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                 .orElseThrow(() -> new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND));
        // 본인의 알림인지 확인
        if (!notification.getUser().getId().equals(userId)) {
                 throw new ApiException(ErrorCode.ACCESS_DENIED);
        }
        return NotificationRes.from(notification);
    }

    /**
     * 단건 읽음 처리
     */
    @Transactional
    public NotificationRes markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(()-> new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead(LocalDateTime.now());
        return NotificationRes.from(notification);
    }

    /**
     * 전체 읽음 처리
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllRead(userId,LocalDateTime.now());
    }

    /**
     * 미읽음 개수
     */
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUser_IdAndReadAtIsNull(userId);
    }
}
