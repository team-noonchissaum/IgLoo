package noonchissaum.backend.domain.order.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.order.service.ChargeCheckService;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/charges")
public class ChargeController {

    private final ChargeCheckService chargeCheckService;

    @PostMapping("/{chargeCheckId}/confirm")
    public void confirm(@PathVariable Long chargeCheckId,
                        @AuthenticationPrincipal UserPrincipal user) {
        Long userId = user.getUserId();
        chargeCheckService.confirmCharge(chargeCheckId, userId);
    }
}
