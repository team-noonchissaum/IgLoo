package noonchissaum.backend.domain.chat.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.chat.dto.res.ChatRoomRes;
import noonchissaum.backend.domain.chat.dto.res.MyChatRoomRes;
import noonchissaum.backend.domain.chat.repository.ChatRoomRepository;
import noonchissaum.backend.domain.chat.service.ChatRoomService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

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
        ChatRoomRes res = chatRoomService.getRoom(roomId, userId);
        return ApiResponse.success("채팅방 조회 성공", res);
    }



}
