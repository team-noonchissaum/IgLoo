package noonchissaum.backend.domain.order.controller;
import lombok.RequiredArgsConstructor;

import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import noonchissaum.backend.domain.order.dto.delivery.req.ChooseDeliveryTypeReq;
import noonchissaum.backend.domain.order.dto.delivery.res.ChooseDeliveryTypeRes;
import noonchissaum.backend.domain.order.service.OrderService;
import noonchissaum.backend.global.dto.ApiResponse;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

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
