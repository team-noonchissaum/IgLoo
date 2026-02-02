package noonchissaum.backend.domain.order.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.order.dto.payment.req.TossWebhookReq;
import noonchissaum.backend.domain.order.service.PaymentService;
import noonchissaum.backend.global.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;

    /**
     * 가상계좌 입금 완료시 토스에서 웹훅으로 알림을 줌
     * */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> handleWebhook(@RequestBody TossWebhookReq req) {
        // 입금 완료시에만 동작
        if ("PAYMENT_STATUS_CHANGED".equals(req.eventType()) &&
        "DONE".equals(req.data().status())) {
            paymentService.processDepositDone(req.data().paymentKey(), req.data().orderId(), req.data().amount());
        }


        return ResponseEntity.ok(ApiResponse.success("success"));
    }
}
