package noonchissaum.backend.domain.notification.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.notification.entity.Notification;
import noonchissaum.backend.domain.notification.repository.NotificationRepository;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import noonchissaum.backend.domain.notification.dto.NotificationResponse;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    /**
     * 알람 전체 리스트
     */
    public List<NotificationResponse> findAllByUserId(Long userId) {
        return notificationRepository.findAllByUserId(userId).stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }


    /**
    * 알림 단건 조회 (본인 확인 포함)
      */
    public NotificationResponse findById(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                 .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
        // 본인의 알림인지 확인
        if (!notification.getUser().getId().equals(userId)) {
                 throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        return NotificationResponse.from(notification);
    }

}
