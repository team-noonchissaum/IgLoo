package noonchissaum.backend.domain.chatbot.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class ChatNodeRes {
    private Long nodeId;
    private Long scenarioId;
    private String text;
    private boolean terminal;
    private List<ChatOptionRes> options;
}
