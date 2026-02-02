package noonchissaum.backend.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.global.entity.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(length = 255)
    private String message;

    @Column(name = "ref_type")
    private String refType; // "AUCTION", "BID", "ORDER"

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Builder
    private Notification(User user, NotificationType type, String message, String refType, Long refId, LocalDateTime readAt) {
        this.user = user;
        this.type = type;
        this.message = message;
        this.refType = refType;
        this.refId = refId;
    }
    public void markAsRead(LocalDateTime now) {
        if(this.readAt == null) {
            this.readAt = now;
        }
    }
}
