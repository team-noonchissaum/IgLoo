package noonchissaum.backend.domain.chat.dto.res;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
@Getter
@AllArgsConstructor
public class ChatMessagePageRes {
    private List<ChatMessageRes> messages;
    private Long nextCursor;
    private boolean hasNext;
}