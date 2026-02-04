package noonchissaum.backend.domain.notification.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.auction.service.AuctionMessageService;
import noonchissaum.backend.domain.notification.constants.NotificationConstants;
import noonchissaum.backend.domain.notification.dto.res.NotificationResponse;
import noonchissaum.backend.domain.notification.entity.Notification;
import noonchissaum.backend.domain.notification.entity.NotificationType;
import noonchissaum.backend.global.dto.SocketMessageType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@RequiredArgsConstructor
@Service
public class AuctionNotificationService {
    private final AuctionRepository auctionRepository;
    private final StringRedisTemplate redisTemplate;
    private final BidRepository bidRepository;
    private final NotificationService notificationService;
    private final AuctionMessageService auctionMessageService;

    /**
     * 알림 생성 및 전송 (공통 메서드)
     */
    @Transactional
    public void sendNotification(Long userId, NotificationType type, String message, String refType, Long refId) {
        // 1. DB 저장 (NotificationService 위임)
        Notification saved = notificationService.create(userId, type, message, refType, refId);

        // 2. WebSocket 전송 (AuctionMessageService 활용)
        NotificationResponse response = NotificationResponse.from(saved);
        auctionMessageService.sendToUserQueue(userId, SocketMessageType.NOTIFICATION, response);
    }

    /**
     * 마감 임박 알림 (1:N)
     * 참여자 전원에게 알림 저장 후 WS 푸시
     */
    @Transactional
    public void notifyImminent(Long auctionId) {

        //중복 방지 키
        String dedupKey = "notify:auction:" + auctionId + ":imminent";
        Boolean first = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", Duration.ofMinutes(5));
        if (Boolean.FALSE.equals(first)) return;

        // 1. 판매자에게 알림
        auctionRepository.findById(auctionId).ifPresent(auction -> {
            sendNotification(
                    auction.getSeller().getId(),
                    NotificationType.IMMINENT,
                    NotificationConstants.MSG_AUCTION_IMMINENT,
                    NotificationConstants.REF_TYPE_AUCTION,
                    auctionId
            );
        });

        // 2. 참여자 전원에게 알림
        List<Long> participantIds = bidRepository.findDistinctBidderIdsByAuctionId(auctionId);
        for (Long userId : participantIds) {
            sendNotification(
                    userId,
                    NotificationType.IMMINENT,
                    NotificationConstants.MSG_AUCTION_IMMINENT,
                    NotificationConstants.REF_TYPE_AUCTION,
                    auctionId
            );
        }
    }
}




