package noonchissaum.backend.domain.auction.controller;


import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.dto.ws.AuctionSnapshotPayload;
import noonchissaum.backend.domain.auction.service.AuctionRealtimeSnapshotService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Controller
@RequiredArgsConstructor
public class AuctionWsController {

    private final AuctionRealtimeSnapshotService snapshotService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/auctions/{auctionId}/snapshot")
    public void snapshot(@DestinationVariable Long auctionId) {

        AuctionSnapshotPayload payload = snapshotService.getSnapshot(auctionId);
        Long userId = 1L;
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/auctions/" + auctionId + "/snapshot",
                payload
        );
    }
}
