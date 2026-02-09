package noonchissaum.backend.domain.chatbot.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import noonchissaum.backend.domain.chatbot.entity.ChatActionType;

@Getter
@AllArgsConstructor
public class ChatOptionRes {
    private Long optionId;
    private String label;
    private Long nextNodeId;
    private ChatActionType actionType;
    private String actionTarget;
}
