package noonchissaum.backend.domain.chat.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import noonchissaum.backend.domain.chat.entity.ChatRoom;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatRoomRes {

    private Long roomId;
    private Long auctionId;

    private Long buyerId;
    private Long sellerId;

    private Long myUserId;
    private String myRole;        // BUYER / SELLER

    private Long opponentId;
    private String opponentRole;  // BUYER / SELLER

    private LocalDateTime createdAt;

    public static ChatRoomRes from(ChatRoom room, Long myUserId) {
        boolean isBuyer = room.getBuyer().getId().equals(myUserId);

        return new ChatRoomRes(
                room.getId(),
                room.getAuction().getId(),
                room.getBuyer().getId(),
                room.getSeller().getId(),
                myUserId,
                isBuyer ? "BUYER" : "SELLER",
                isBuyer ? room.getSeller().getId() : room.getBuyer().getId(),
                isBuyer ? "SELLER" : "BUYER",
                room.getCreatedAt()
        );
    }
}
