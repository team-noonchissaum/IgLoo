package noonchissaum.backend.domain.order.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.order.dto.charge.req.ChargeCancelReq;
import noonchissaum.backend.domain.order.service.ChargeCheckService;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public void confirm(@PathVariable Long chargeCheckId,
                        @AuthenticationPrincipal UserPrincipal user) {
        Long userId = user.getUserId();
        chargeCheckService.confirmCharge(chargeCheckId, userId);
    }

    /**
     * 충전 취소(환불)
     * - userLock 획득 후 checkTasks 를 통해 정합성 체크.
     */
    @PostMapping("/{chargeCheckId}/cancel")
    public void cancel(@PathVariable Long chargeCheckId,
                       @AuthenticationPrincipal UserPrincipal user,
                       @RequestBody ChargeCancelReq req
                       ) {
        Long userId = user.getUserId();
        chargeCheckService.cancelCharge(chargeCheckId, userId,req.cancelReason());
    }
}
