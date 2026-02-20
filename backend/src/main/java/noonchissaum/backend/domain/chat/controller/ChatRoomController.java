package noonchissaum.backend.domain.chat.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.chat.dto.res.ChatMessagePageRes;
import noonchissaum.backend.domain.chat.dto.res.ChatRoomRes;
import noonchissaum.backend.domain.chat.dto.res.MyChatRoomRes;
import noonchissaum.backend.domain.chat.service.ChatMessageService;
import noonchissaum.backend.domain.chat.service.ChatRoomService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @GetMapping("/rooms")
    public ApiResponse<List<MyChatRoomRes>> myRooms(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();

        List<MyChatRoomRes> res = chatRoomService.getMyRooms(userId);
        return ApiResponse.success("채팅방 목록 조회 성공", res);
    }

    @GetMapping("/rooms/{roomId}")
    public ApiResponse<ChatRoomRes> room(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        ChatRoomRes res = chatRoomService.getRoom(roomId, userPrincipal);
        return ApiResponse.success("채팅방 조회 성공", res);
    }

    @PostMapping("/room/{roomId}/enter")
    public ApiResponse<Integer> enterRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ){
        Long userId = userPrincipal.getUserId();
        int updated = chatMessageService.ReadMessage(roomId, userId);
        return ApiResponse.success("읽음 처리 완료", updated);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ApiResponse<ChatMessagePageRes> messages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = userPrincipal.getUserId();
        ChatMessagePageRes res = chatMessageService.getMessages(roomId, userId, cursor, size);
        return ApiResponse.success("채팅 메시지 조회 성공", res);
    }

    /**
     * 경매 기준 채팅방 생성 또는 조회
     * - 구매자: DIRECT 선택 시 채팅방 생성 후 반환
     * - 판매자: 채팅방이 있으면 반환
     */
    @PostMapping("/rooms/from-auction/{auctionId}")
    public ApiResponse<ChatRoomRes> ensureRoomFromAuction(
            @PathVariable Long auctionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        ChatRoomRes res = chatRoomService.ensureRoomForAuction(auctionId, userId);
        return ApiResponse.success("채팅방 조회/생성 성공", res);
    }
}
