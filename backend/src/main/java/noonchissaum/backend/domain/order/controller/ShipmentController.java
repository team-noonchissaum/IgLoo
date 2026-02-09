package noonchissaum.backend.domain.order.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.order.dto.shipment.req.SaveAddressReq;
import noonchissaum.backend.domain.order.dto.shipment.req.ShipmentReq;
import noonchissaum.backend.domain.order.dto.shipment.res.ShipmentRes;
import noonchissaum.backend.domain.order.service.ShipmentService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class ShipmentController {
    private final ShipmentService shipmentService;

    @PostMapping("/{orderId}/shipment")
    public ApiResponse<ShipmentRes> requestShipment(
            @PathVariable Long orderId
    ){
        ShipmentRes res = shipmentService.requestShipment(orderId);
        return ApiResponse.success("배송 정보 생성 완료", res);
    }

    // 구매자: 배송요청 입력
    @PostMapping("/{orderId}/shipment/address")
    public ApiResponse<ShipmentRes> saveAddress(
            @PathVariable Long orderId,
            @RequestBody SaveAddressReq saveAddressReq,
            @AuthenticationPrincipal UserPrincipal userPrincipal
            ){
        Long userId = userPrincipal.getUserId();
        ShipmentRes res = shipmentService.saveAddress(orderId, userId, saveAddressReq);
        return ApiResponse.success("배송지 저장 완료", res);
    }

    // 판매자: 송장 등록
    @PatchMapping("/{orderId}/shipment/tracking")
    public ApiResponse<ShipmentRes> registerTracking(
            @PathVariable Long orderId,
            @RequestBody ShipmentReq shipmentReq,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        ShipmentRes res = shipmentService.registerTracking(orderId, userId, shipmentReq);
        return ApiResponse.success("송장 등록 완료", res);
    }

    // 배송 조회
    @GetMapping("/{orderId}/shipment")
    public ApiResponse<ShipmentRes> getShipment(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        ShipmentRes res = shipmentService.getShipment(orderId, userId);
        return ApiResponse.success("배송 조회 성공", res);
    }
}
