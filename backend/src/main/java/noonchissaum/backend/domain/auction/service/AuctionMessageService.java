package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.dto.ws.*;
import noonchissaum.backend.global.dto.SocketMessageType;
import noonchissaum.backend.global.dto.WsMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuctionMessageService {
    private final SimpMessagingTemplate messagingTemplate;

    /**
    * 경매 룸 브로드캐스트(topic)
    * /topic/auction/{auctionId} */
    public <T> void sendToAuctionTopic(Long auctionId, SocketMessageType type, T payload){
        String destination = "/topic/auction/" + auctionId;
        messagingTemplate.convertAndSend(destination, WsMessage.of(type, payload));
    }

    /**
     * 개인 알림 (queue)
     * /queue/notifications */
    public <T> void sendToUserQueue(Long userId, SocketMessageType type, T payload){
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/notifications",
                WsMessage.of(type, payload)
        );
    }

    // 입찰 성공 브로드캐스트
    public void sendBidSucceeded(Long auctionId, BidSucceededPayload payload) {
        sendToAuctionTopic(auctionId, SocketMessageType.BID_SUCCESSED, payload);
    }

    // 이전 최고 입찰자에게 OUTBID 알림
    public void sendOutbid(Long userId, OutbidPayload payload) {
        sendToUserQueue(userId, SocketMessageType.OUTBID, payload);
    }
    public void sendAuctionExtended(Long auctionId, AuctionExtendedPayload payload){
        sendToAuctionTopic(auctionId, SocketMessageType.AUCTION_EXTENDED, payload);
    }

    // 경매 전체 스냅샷 브로드캐스트 (주기적 중계용)
    public void sendAuctionSnapshot(Long auctionId, Object payload) {
        sendToAuctionTopic(auctionId, SocketMessageType.NOTIFICATION, payload);
    }

    // 경매 종료 브로드 캐스트
    public void sendAuctionEnded(Long auctionId, AuctionEndedPayload payload) {
        sendToAuctionTopic(auctionId, SocketMessageType.AUCTION_ENDED, payload);
    }

    public void sendAuctionResult(Long auctionId, AuctionResultPayload payload) {
        sendToAuctionTopic(auctionId, SocketMessageType.AUCTION_RESULT, payload);
    }

}
