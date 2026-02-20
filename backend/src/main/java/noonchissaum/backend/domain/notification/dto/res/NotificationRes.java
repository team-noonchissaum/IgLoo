package noonchissaum.backend.domain.notification.dto.res;

import lombok.Builder;
import lombok.Getter;
import noonchissaum.backend.domain.notification.entity.Notification;
import noonchissaum.backend.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationRes {
    private Long id;
    private NotificationType type;
    private String message;
    private String refType;
    private Long refId;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public static NotificationRes from(Notification notification) {
        return NotificationRes.builder()
                .id(notification.getId())
                .type(notification.getType())
                .message(notification.getMessage())
                .refType(notification.getRefType())
                .refId(notification.getRefId())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
