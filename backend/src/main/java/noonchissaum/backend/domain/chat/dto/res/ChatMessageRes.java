package noonchissaum.backend.domain.chat.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import noonchissaum.backend.domain.chat.entity.ChatMessage;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatMessageRes {
    private Long messageId;
    private Long roomId;
    private Long senderId;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static ChatMessageRes from(ChatMessage message) {
        return new ChatMessageRes(
                message.getId(),
                message.getRoom().getId(),
                message.getSender().getId(),
                message.getMessage(),
                message.getIsRead(),
                message.getCreatedAt()
        );
    }
}