package noonchissaum.backend.domain.order.controller;
import lombok.RequiredArgsConstructor;

import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import noonchissaum.backend.domain.order.dto.delivery.req.ChooseDeliveryTypeReq;
import noonchissaum.backend.domain.order.dto.delivery.res.ChooseDeliveryTypeRes;
import noonchissaum.backend.domain.order.dto.delivery.res.OrderByAuctionRes;
import noonchissaum.backend.domain.order.entity.Order;
import noonchissaum.backend.domain.order.entity.DeliveryType;
import noonchissaum.backend.domain.order.repository.OrderRepository;
import noonchissaum.backend.domain.chat.service.ChatRoomService;
import noonchissaum.backend.domain.chat.repository.ChatRoomRepository;
import noonchissaum.backend.domain.chat.dto.res.ChatRoomRes;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;

import noonchissaum.backend.domain.order.service.OrderService;
import noonchissaum.backend.global.dto.ApiResponse;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final ChatRoomService chatRoomService;
    private final ChatRoomRepository chatRoomRepository;

    /**
     * 경매 기준 주문 조회 (구매자 또는 판매자)
     */
    @GetMapping("/by-auction/{auctionId}")
    public ResponseEntity<OrderByAuctionRes> getOrderByAuction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long auctionId
    ) {
        Long userId = principal.getUserId();

        Order order = orderRepository.findByAuction_IdAndBuyer_Id(auctionId, userId)
                .or(() -> orderRepository.findByAuction_IdAndSeller_Id(auctionId, userId))
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        Long roomId = null;
        if (order.getDeliveryType() == DeliveryType.DIRECT) {
            roomId = chatRoomRepository.findByAuctionId(auctionId)
                    .map(room -> room.getId())
                    .orElse(null);
        }

        return ResponseEntity.ok(new OrderByAuctionRes(
                order.getId(),
                order.getDeliveryType(),
                roomId
        ));
    }

    /**
     * 거래 방식 선택
     * - 구매자만 가능
     * - DIRECT면 채팅방 생성 후 roomId 반환
     * - SHIPMENT면 roomId=null
     */
    // 컨트롤러에서 서비스 호출로 변경
    @PatchMapping("/{orderId}/delivery-type")
    public ApiResponse<ChooseDeliveryTypeRes> chooseDeliveryType(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId,
            @RequestBody ChooseDeliveryTypeReq req
    ) {
        Long userId = principal.getUserId();
        ChooseDeliveryTypeRes res = orderService.chooseDeliveryType(orderId, userId, req);
        return ApiResponse.success("거래 방식 선택 완료", res);
    }
    /** 구매자: 배송완료 후 구매확정 */
    @PatchMapping("/{orderId}/shipment/confirm")
    public ApiResponse<Void> confirmDelivered(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        orderService.confirmAfterDelivered(orderId, userPrincipal.getUserId());
        return ApiResponse.success("구매확정 완료", null);
    }

    /**
     * 직거래: 구매확정 */
    @PatchMapping("/{orderId}/direct/confirm")
    public ApiResponse<Void> confirmDirect(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        orderService.confirmDirectTrade(orderId, userPrincipal.getUserId());
        return ApiResponse.success("구매확정 완료", null);
    }

}
