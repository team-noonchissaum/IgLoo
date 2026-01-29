package noonchissaum.backend.domain.auction.dto.ws;

import lombok.*;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutbidPayload {
    private long auctionId;

    // 내가 마지막으로 넣었던 금액
    private BigDecimal myBidPrice;

    // 새로 갱신된 현재 최고가
    private BigDecimal newCurrentPrice;

    // 화면에 띄울 메세지
    private String message;
}
