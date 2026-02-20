package noonchissaum.backend.domain.order.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.order.dto.charge.req.ChargeCancelReq;
import noonchissaum.backend.domain.order.dto.charge.res.ChargeCheckRes;
import noonchissaum.backend.domain.order.service.ChargeCheckService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/charges")
public class ChargeController {

    private final ChargeCheckService chargeCheckService;

    /**
     * 충전 승인
     * - userLock 획득 후 checkTasks 를 통해 정합성 체크.
     */
    @PostMapping("/{chargeCheckId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirm(@PathVariable Long chargeCheckId,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        chargeCheckService.confirmCharge(chargeCheckId, userId);
        return ResponseEntity.ok(ApiResponse.success("success"));
    }

    /**
     * 충전 취소(환불)
     * - userLock 획득 후 checkTasks 를 통해 정합성 체크.
     */
    @PostMapping("/{chargeCheckId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long chargeCheckId,
                       @AuthenticationPrincipal UserPrincipal user,
                       @RequestBody ChargeCancelReq req
                       ) {
        Long userId = user.getUserId();
        chargeCheckService.cancelCharge(chargeCheckId, userId,req.cancelReason());
        return ResponseEntity.ok(ApiResponse.success("success"));
    }

    @GetMapping("/unchecked")
    public ResponseEntity<ApiResponse<List<ChargeCheckRes>>> getUnchecked(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<ChargeCheckRes> list = chargeCheckService.getUncheckedList(userPrincipal.getUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("unchecked list", list));
    }
}
