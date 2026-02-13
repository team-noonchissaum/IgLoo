package noonchissaum.backend.domain.chat.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.chat.dto.res.ChatMessagePageRes;
import noonchissaum.backend.domain.chat.dto.res.ChatRoomRes;
import noonchissaum.backend.domain.chat.service.ChatMessageService;
import noonchissaum.backend.domain.chat.service.ChatRoomService;
import noonchissaum.backend.global.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/chat")
public class AdminChatController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @GetMapping("/rooms/{roomId}")
    public ApiResponse<ChatRoomRes> getRoomDetail(@PathVariable Long roomId) {
        return ApiResponse.success("관리자용 채팅방 상세 조회 성공",
                chatRoomService.getRoomForAdmin(roomId));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ApiResponse<ChatMessagePageRes> getMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        ChatMessagePageRes res = chatMessageService.getMessagesForAdmin(roomId, cursor, size);
        return ApiResponse.success("관리자용 메시지 내역 조회 성공", res);
    }
}
