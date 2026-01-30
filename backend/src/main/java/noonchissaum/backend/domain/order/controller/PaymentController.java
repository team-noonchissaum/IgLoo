package noonchissaum.backend.domain.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.order.dto.payment.req.PaymentAbortReq;
import noonchissaum.backend.domain.order.dto.payment.req.PaymentConfirmReq;
import noonchissaum.backend.domain.order.dto.payment.req.PaymentPrepareReq;
import noonchissaum.backend.domain.order.dto.payment.res.PaymentPrepareRes;
import noonchissaum.backend.domain.order.dto.payment.res.VirtualAccountInfo;
import noonchissaum.backend.domain.order.service.PaymentService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 준비
     * POST /api/payments/prepare
     * */
    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<PaymentPrepareRes>> createPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PaymentPrepareReq req
            ){
        PaymentPrepareRes paymentPrepareRes = paymentService
                .preparePayment(principal.getUserId(), req.amount(), req.provider());


        return ResponseEntity.ok(ApiResponse.success("준비 성공", paymentPrepareRes));
    }

    /**
     * 결제 승인 반영
     * */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<VirtualAccountInfo>> confirmPayment(
            @Valid @RequestBody PaymentConfirmReq req
    ) {
        VirtualAccountInfo info = paymentService.confirmPayment(req.pgOrderId(), req.paymentKey(), req.amount());
        if (info != null) {
            return ResponseEntity.ok(ApiResponse.success("가상계좌 발급 완료", info));
        }
        return ResponseEntity.ok(ApiResponse.success("승인 성공", null));
    }

    /**
     * 결제 실패 반영
     *
     */
    @PostMapping("/abort")
    public ResponseEntity<ApiResponse<Void>> abortPayment(@RequestBody PaymentAbortReq req) {
        paymentService.abortPayment(req.paymentId(), req.reason());
        return ResponseEntity.ok(ApiResponse.success("결제 실패"));
    }
}
