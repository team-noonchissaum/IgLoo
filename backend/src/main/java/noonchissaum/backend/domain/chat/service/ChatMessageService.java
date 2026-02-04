package noonchissaum.backend.domain.chat.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.chat.dto.ws.ChatSendReq;
import noonchissaum.backend.domain.chat.repository.ChatMessageRepository;
import noonchissaum.backend.domain.chat.repository.ChatRoomRepository;
import org.springframework.stereotype.Service;
import noonchissaum.backend.domain.user.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import noonchissaum.backend.domain.chat.dto.ws.ChatMessagePayload;
import noonchissaum.backend.domain.chat.entity.ChatRoom;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.chat.entity.ChatMessage;
import noonchissaum.backend.domain.user.service.UserService;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    //채팅 메세지 전송
    @Transactional
    public ChatMessagePayload sendMessage(Long roomId, Long senderId, String message){
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다. roomId=" + roomId));

        // 권한 체크: buyer/seller만 가능
        if (!room.getBuyer().getId().equals(senderId)
                && !room.getSeller().getId().equals(senderId)) {
            throw new IllegalArgumentException("채팅 권한이 없습니다.");
        }
        User sender = userService.getUserByUserId(senderId);

        ChatMessage saved = chatMessageRepository.save(
                ChatMessage.builder()
                        .room(room)
                        .sender(sender)
                        .message(message)
                        .isRead(false)
                        .build()
        );

        ChatMessagePayload payload = new ChatMessagePayload(
                room.getId(),
                saved.getId(),
                senderId,
                saved.getMessage(),
                saved.getCreatedAt()
        );
        // 브로드캐스트
        messagingTemplate.convertAndSend("/topic/chat." + room.getId(), payload);

        return payload;
    }


}
