package noonchissaum.backend.domain.notification.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.auction.service.AuctionMessageService;
import noonchissaum.backend.domain.notification.dto.ws.NotificationPayload;
import noonchissaum.backend.domain.notification.entity.Notification;
import noonchissaum.backend.domain.notification.entity.NotificationType;
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
     * 마감 임박 알림 (1:N)
     * 참여자 전원에게 알림 저장 후 WS 푸시
     */
    @Transactional
    public void notifyImminent(Long auctionId) {

        //중복 방지 키
        String dedupKey = "notify:auction:" + auctionId + ":imminent";
        Boolean first = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", Duration.ofHours(1));
        if (Boolean.FALSE.equals(first)) return;

        List<Long> participantIds = bidRepository.findDistinctBidderIdsByAuctionId(auctionId);
        if (participantIds.isEmpty()) return;

        String msg = "경매 마감이 임박했습니다.";

        for (Long userId : participantIds) {
            Notification saved = notificationService.create(
                    userId,
                    NotificationType.IMMINENT,
                    msg,
                    "AUCTION",
                    auctionId
            );

            //WS 푸시
            NotificationPayload payload = new NotificationPayload(
                    saved.getId(),
                    saved.getType().name(),
                    saved.getMessage(),
                    saved.getRefType(),
                    saved.getRefId(),
                    saved.getCreatedAt()
            );
            auctionMessageService.sendToUserQueue(userId, noonchissaum.backend.global.dto.SocketMessageType.NOTIFICATION, payload);
        }
    }
}




