package noonchissaum.backend.domain.chat.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.chat.repository.ChatMessageRepository;
import noonchissaum.backend.domain.chat.repository.ChatRoomRepository;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import noonchissaum.backend.domain.chat.dto.ws.ChatMessagePayload;
import noonchissaum.backend.domain.chat.entity.ChatRoom;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.chat.entity.ChatMessage;
import noonchissaum.backend.domain.user.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import noonchissaum.backend.domain.chat.dto.res.ChatMessagePageRes;
import noonchissaum.backend.domain.chat.dto.res.ChatMessageRes;
import java.util.ArrayList;
import java.util.List;

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

    @Transactional(readOnly = true)
    public ChatMessagePageRes getMessages(Long roomId, Long userId, Long cursor, int size) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다. roomId=" + roomId));

        if (!room.getBuyer().getId().equals(userId)
                && !room.getSeller().getId().equals(userId)) {
            throw new IllegalArgumentException("채팅 권한이 없습니다.");
        }

        int pageSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1, Sort.by(Sort.Direction.DESC, "id"));

        List<ChatMessage> fetched = cursor == null
                ? chatMessageRepository.findByRoomIdOrderByIdDesc(roomId, pageRequest)
                : chatMessageRepository.findByRoomIdAndIdLessThanOrderByIdDesc(roomId, cursor, pageRequest);

        boolean hasNext = fetched.size() > pageSize;
        List<ChatMessage> pageItems = hasNext ? fetched.subList(0, pageSize) : fetched;

        List<ChatMessageRes> messages = new ArrayList<>();
        for (ChatMessage message : pageItems) {
            messages.add(ChatMessageRes.from(message));
        }

        Long nextCursor = messages.isEmpty() ? null : messages.get(messages.size() - 1).getMessageId();

        return new ChatMessagePageRes(messages, nextCursor, hasNext);
    }

    /**
     * 채팅방 입장 시 상대방이 보낸 메시지 읽음 처리
     * */
    @Transactional
    public int ReadMessage(Long roomId, Long userId){
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        boolean isMember = room.getBuyer().getId().equals(userId) || room.getSeller().getId().equals(userId);

        if (!isMember){
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }
        return chatMessageRepository.markAllAsReadInRoom(roomId, userId);
    }
}
