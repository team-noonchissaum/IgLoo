package noonchissaum.backend.domain.order.controller;
import lombok.RequiredArgsConstructor;

import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import noonchissaum.backend.domain.order.dto.delivery.req.ChooseDeliveryTypeReq;
import noonchissaum.backend.domain.order.dto.delivery.res.ChooseDeliveryTypeRes;
import noonchissaum.backend.domain.order.entity.Order;
import noonchissaum.backend.domain.order.entity.DeliveryType;
import noonchissaum.backend.domain.order.repositroy.OrderRepository;
import noonchissaum.backend.domain.chat.service.ChatRoomService;
import noonchissaum.backend.domain.chat.dto.res.ChatRoomRes;



@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderRepository orderRepository;
    private final ChatRoomService chatRoomService;

    /**
     * 거래 방식 선택
     * - 구매자만 가능
     * - DIRECT면 채팅방 생성 후 roomId 반환
     * - SHIPMENT면 roomId=null
     */
    @PatchMapping("/{orderId}/delivery-type")
    public ResponseEntity<ChooseDeliveryTypeRes> chooseDeliveryType(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId,
            @RequestBody ChooseDeliveryTypeReq req
    ) {
        Long userId = principal.getUserId();

        Order order = orderRepository.findByIdAndBuyerId(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("권한 없음 또는 주문 없음"));

        order.chooseDeliveryType(req.type());

        Long roomId = null;
        if (req.type() == DeliveryType.DIRECT) {
            ChatRoomRes res = chatRoomService.createRoom(order);
            roomId = res.getRoomId();
        }

        return ResponseEntity.ok(new ChooseDeliveryTypeRes(order.getId(), order.getDeliveryType(), roomId));
    }
}
