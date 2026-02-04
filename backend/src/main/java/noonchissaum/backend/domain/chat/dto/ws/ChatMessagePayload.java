package noonchissaum.backend.domain.chat.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatMessagePayload {
    private Long roomId;
    private Long messageId;
    private Long senderId;
    private String message;
    private LocalDateTime createdAt;
}
