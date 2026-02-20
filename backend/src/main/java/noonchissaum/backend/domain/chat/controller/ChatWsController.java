package noonchissaum.backend.domain.chat.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.chat.dto.ws.ChatSendReq;
import noonchissaum.backend.domain.chat.service.ChatMessageService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWsController {
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat/rooms/{roomId}/messages")
    public void send(
            @DestinationVariable Long roomId,
            @Payload ChatSendReq chatSendReq,
            Principal principal
            ){

        Long senderId = Long.parseLong(principal.getName());
        chatMessageService.sendMessage(roomId, senderId, chatSendReq.getMessage());
    }
}
