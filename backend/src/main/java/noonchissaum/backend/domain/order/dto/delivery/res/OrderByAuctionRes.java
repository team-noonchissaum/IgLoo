package noonchissaum.backend.domain.order.dto.delivery.res;

import noonchissaum.backend.domain.order.entity.DeliveryType;

/**
 * 경매 기준 주문 조회 응답 (구매자/판매자용)
 */
public record OrderByAuctionRes(
        Long orderId,
        DeliveryType deliveryType,
        Long roomId
) {
}
