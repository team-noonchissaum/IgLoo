package noonchissaum.backend.domain.chat.dto.ws;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatSendReq {
    private String message;
}
