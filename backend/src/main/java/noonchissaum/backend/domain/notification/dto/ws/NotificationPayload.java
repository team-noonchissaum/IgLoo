package noonchissaum.backend.domain.notification.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPayload {
    private Long notificationId;
    private String type;
    private String message;
    private String refType;
    private Long refId;
    private LocalDateTime createdAt;
}
