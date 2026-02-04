package noonchissaum.backend.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.chat.dto.res.ChatRoomRes;
import noonchissaum.backend.domain.chat.dto.res.MyChatRoomRes;
import noonchissaum.backend.domain.chat.entity.ChatRoom;
import noonchissaum.backend.domain.chat.repository.ChatRoomRepository;
import noonchissaum.backend.domain.order.entity.Order;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;

    /**
     * DIRECT 거래 선택 시 채팅방 생성
     * auctionId 기준으로 UNIQUE를 걸어두면 "경매당 채팅방 1개"가 보장되며 중복 클릭/재시도/동시 요청에도 멱등하게 동작한다.
     * deliveryType == DIRECT 인 경우에만 호출
     */
    @Transactional
    public ChatRoomRes createRoom(Order order){
        validateOrderForDirectTrade(order);

        Long auctionId = order.getAuction().getId();
        Long myUserId = order.getBuyer().getId(); // DIRECT 선택 주체 = buyer 전제

        return chatRoomRepository.findByAuctionId(auctionId)
                .map(room -> ChatRoomRes.from(room, myUserId))
                .orElseGet(() -> createRoomWithDedup(order, myUserId));
    }

    private ChatRoomRes createRoomWithDedup(Order order, Long myUserId) {
        Long auctionId = order.getAuction().getId();

        try {
            ChatRoom room = ChatRoom.builder()
                    .auction(order.getAuction())
                    .seller(order.getSeller())
                    .buyer(order.getBuyer())
                    .build();

            ChatRoom saved = chatRoomRepository.save(room);
            return ChatRoomRes.from(saved, myUserId);

        } catch (DataIntegrityViolationException e) {
            log.info("ChatRoom unique conflict (auctionId={}), re-fetching...", auctionId);

            return chatRoomRepository.findByAuctionId(auctionId)
                    .map(r -> ChatRoomRes.from(r, myUserId))
                    .orElseThrow(() -> e);
        }
    }

    @Transactional(readOnly = true)
    public List<MyChatRoomRes> getMyRooms(Long userId) {
        List<ChatRoom> rooms = chatRoomRepository.findMyRooms(userId);
        return rooms.stream()
                .map(room -> MyChatRoomRes.from(room, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatRoomRes getRoom(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        boolean isMember =
                room.getBuyer().getId().equals(userId) ||
                        room.getSeller().getId().equals(userId);

        if (!isMember) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        return ChatRoomRes.from(room, userId);
    }


    /**
    * DIRECT 채팅방 생성이 가능한 Order인지 검증
    * Order, auction, buyer, seller가 존재해야 함
    */
    private void validateOrderForDirectTrade(Order order) {
        if (order == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        if (order.getAuction() == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        if (order.getBuyer() == null || order.getSeller() == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }
}
