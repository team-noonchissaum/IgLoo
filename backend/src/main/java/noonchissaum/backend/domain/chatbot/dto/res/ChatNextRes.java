package noonchissaum.backend.domain.chatbot.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatNextRes {
    private String type;
    private ChatNodeRes node;
    private ChatActionRes action;
}
