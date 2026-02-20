package noonchissaum.backend.domain.chat.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import noonchissaum.backend.domain.chat.entity.ChatRoom;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MyChatRoomRes {
    private Long roomId;
    private Long auctionId;
    private Long opponentId;      // 상대방 userId
    private String opponentRole;  // BUYER / SELLER
    private LocalDateTime createdAt;

    public static MyChatRoomRes from(ChatRoom room, Long myUserId) {
        boolean isBuyer = room.getBuyer().getId().equals(myUserId);

        return new MyChatRoomRes(
                room.getId(),
                room.getAuction().getId(),
                isBuyer ? room.getSeller().getId() : room.getBuyer().getId(),
                isBuyer ? "SELLER" : "BUYER",
                room.getCreatedAt()
        );
    }
}