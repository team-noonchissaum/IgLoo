package noonchissaum.backend.domain.chatbot.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ChatNextReq {
    @NotNull
    private Long nodeId;

    @NotNull
    private Long optionId;
}
