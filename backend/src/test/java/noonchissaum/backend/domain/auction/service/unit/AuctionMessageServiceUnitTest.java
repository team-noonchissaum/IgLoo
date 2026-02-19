package noonchissaum.backend.domain.auction.service.unit;

import noonchissaum.backend.domain.auction.dto.ws.BidSucceededPayload;
import noonchissaum.backend.domain.auction.dto.ws.OutbidPayload;
import noonchissaum.backend.domain.auction.service.AuctionMessageService;
import noonchissaum.backend.global.dto.SocketMessageType;
import noonchissaum.backend.global.dto.WsMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuctionMessageServiceUnitTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    @DisplayName("입찰 성공 메시지는 경매 토픽으로 전송")
    void sendBidSucceeded_sendsTopicMessage() {
        AuctionMessageService service = new AuctionMessageService(messagingTemplate);
        BidSucceededPayload payload = BidSucceededPayload.builder().auctionId(7L).currentPrice(1000L).build();

        service.sendBidSucceeded(7L, payload);

        ArgumentCaptor<WsMessage> captor = ArgumentCaptor.forClass(WsMessage.class);
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/auction/7"), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(SocketMessageType.BID_SUCCESSED);
        assertThat(captor.getValue().getPayload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("아웃비드 메시지는 사용자 큐로 전송")
    void sendOutbid_sendsUserQueueMessage() {
        AuctionMessageService service = new AuctionMessageService(messagingTemplate);
        OutbidPayload payload = OutbidPayload.builder().auctionId(9L).build();

        service.sendOutbid(33L, payload);

        ArgumentCaptor<WsMessage> captor = ArgumentCaptor.forClass(WsMessage.class);
        verify(messagingTemplate).convertAndSendToUser(org.mockito.ArgumentMatchers.eq("33"), org.mockito.ArgumentMatchers.eq("/queue/notifications"), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(SocketMessageType.OUTBID);
        assertThat(captor.getValue().getPayload()).isEqualTo(payload);
    }
}
