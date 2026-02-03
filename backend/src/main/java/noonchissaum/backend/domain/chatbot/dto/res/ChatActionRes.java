package noonchissaum.backend.domain.chatbot.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import noonchissaum.backend.domain.chatbot.entity.ChatActionType;

@Getter
@AllArgsConstructor
public class ChatActionRes {
    private ChatActionType actionType;
    private String actionTarget;
}
