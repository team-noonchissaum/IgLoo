package noonchissaum.backend.domain.notification.service;

import noonchissaum.backend.domain.notification.entity.Notification;
import noonchissaum.backend.domain.notification.entity.NotificationType;
import noonchissaum.backend.domain.notification.repository.NotificationRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserService userService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("알림 생성 시 사용자 조회 후 저장한다")
    void create_savesNotification() {
        User user = user(1L, "u@test.com", "user");
        given(userService.getUserByUserId(1L)).willReturn(user);
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            ReflectionTestUtils.setField(n, "id", 99L);
            return n;
        });

        Notification result = notificationService.create(1L, NotificationType.IMMINENT, "메시지", "AUCTION", 10L);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getType()).isEqualTo(NotificationType.IMMINENT);
        assertThat(result.getRefId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("sendNotification은 저장 후 사용자 큐로 웹소켓 푸시한다")
    void sendNotification_pushesToUserQueue() {
        User user = user(2L, "u2@test.com", "user2");
        Notification saved = notification(user, 123L, NotificationType.OUTBID, "아웃비드", "AUCTION", 55L, null);

        given(userService.getUserByUserId(2L)).willReturn(user);
        given(notificationRepository.save(any(Notification.class))).willReturn(saved);

        var result = notificationService.sendNotification(2L, NotificationType.OUTBID, "아웃비드", "AUCTION", 55L);

        assertThat(result.getId()).isEqualTo(123L);
        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/notifications"), any());
    }

    @Test
    @DisplayName("알림 단건 조회 시 본인 알림이 아니면 ACCESS_DENIED")
    void findById_throwsWhenNotOwner() {
        User owner = user(10L, "owner@test.com", "owner");
        Notification n = notification(owner, 1L, NotificationType.IMMINENT, "m", "AUCTION", 1L, null);
        given(notificationRepository.findById(1L)).willReturn(Optional.of(n));

        assertThatThrownBy(() -> notificationService.findById(20L, 1L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("단건 읽음 처리 시 readAt이 갱신된다")
    void markAsRead_setsReadAt() {
        User user = user(3L, "u3@test.com", "user3");
        Notification n = notification(user, 3L, NotificationType.IMMINENT, "m", "AUCTION", 99L, null);
        given(notificationRepository.findByIdAndUserId(3L, 3L)).willReturn(Optional.of(n));

        var result = notificationService.markAsRead(3L, 3L);

        assertThat(result.getReadAt()).isNotNull();
        assertThat(n.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("전체 읽음 처리 시 repository 결과를 그대로 반환한다")
    void markAllAsRead_returnsUpdatedCount() {
        given(notificationRepository.markAllRead(eq(7L), any(LocalDateTime.class))).willReturn(4);

        int updated = notificationService.markAllAsRead(7L);

        assertThat(updated).isEqualTo(4);
    }

    @Test
    @DisplayName("알림 목록 조회는 DTO 리스트로 변환된다")
    void findAllByUserId_returnsMappedList() {
        User user = user(8L, "u8@test.com", "user8");
        Notification n1 = notification(user, 11L, NotificationType.IMMINENT, "m1", "AUCTION", 101L, null);
        Notification n2 = notification(user, 12L, NotificationType.OUTBID, "m2", "AUCTION", 102L, LocalDateTime.now());
        given(notificationRepository.findAllByUserId(8L)).willReturn(List.of(n1, n2));

        var result = notificationService.findAllByUserId(8L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(11L);
        assertThat(result.get(1).getType()).isEqualTo(NotificationType.OUTBID);
    }

    private User user(Long id, String email, String nickname) {
        User user = User.builder().email(email).nickname(nickname).role(UserRole.USER).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Notification notification(User user, Long id, NotificationType type, String message, String refType, Long refId, LocalDateTime readAt) {
        Notification n = Notification.builder()
                .user(user)
                .type(type)
                .message(message)
                .refType(refType)
                .refId(refId)
                .build();
        ReflectionTestUtils.setField(n, "id", id);
        ReflectionTestUtils.setField(n, "readAt", readAt);
        return n;
    }
}
