package noonchissaum.backend.domain.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.order.dto.payment.res.PaymentPrepareRes;
import noonchissaum.backend.domain.order.entity.*;
import noonchissaum.backend.domain.order.repositroy.ChargeCheckRepository;
import noonchissaum.backend.domain.order.repositroy.PaymentRepository;
import noonchissaum.backend.domain.toss.TossPaymentsClient;
import noonchissaum.backend.domain.toss.dto.confirm.TossConfirmRes;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TossPaymentsClient tossPaymentsClient;
    private final PaymentRepository paymentRepository;
    private final ChargeCheckRepository chargeCheckRepository;
    private final UserService userService;

    private String generatePgOrderId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 결제 요청 생성
     * Client 요청 정보 저장
     */
    @Transactional
    public PaymentPrepareRes preparePayment(Long userId, int amount, PgProvider pgProvider) {

        User user = userService.getUserByUserId(userId);

        if (amount <= 0) {
            throw new ApiException(ErrorCode.INVALID_PAYMENT_REQUEST);
        }

        String pgOrderId = generatePgOrderId();

        Payment payment = Payment.builder()
                .pgProvider(pgProvider)
                .amount(new BigDecimal(amount))
                .pgOrderId(pgOrderId)
                .user(user)
                .build();

        Payment saved = paymentRepository.save(payment);
        return new PaymentPrepareRes(saved.getId(), pgOrderId);
    }

    /**
     * 결제 성공 시 보관함(ChargeCheck에 저장)
     */
    @Transactional
    public void confirmPayment(String pgOrderId, String paymentKey, Integer amount) {
        log.info("결제 승인 시작: pgOrderId={}, paymentKey={}", pgOrderId, paymentKey);

        Payment payment = paymentRepository.findByPgOrderId(pgOrderId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_PAYMENT_REQUEST));

        // 멱등성: REQUEST 상태 체크
        if (payment.getStatus() != PaymentStatus.REQUEST) {
            throw new ApiException(ErrorCode.INVALID_PAYMENT_REQUEST);
        }

        // 멱등성
        if (chargeCheckRepository.existsByPaymentId(payment.getId())) {
            log.info("이미 charge 생성된 결제: paymentId={}", payment.getId());
            throw new ApiException(ErrorCode.INVALID_PAYMENT_REQUEST);
        }

        // amount 체크 변조 방지
        int serverAmount = payment.getAmount().intValue();
        if (amount == null || amount != serverAmount) {
            throw new ApiException(ErrorCode.INVALID_PAYMENT_REQUEST);
        }

        // 체크 완료 후 승인 api 호출
        TossConfirmRes tossRes;
        try {

            tossRes = tossPaymentsClient.confirm(paymentKey, pgOrderId, amount);

        } catch (HttpStatusCodeException e) {

            String errorBody = e.getResponseBodyAsString();
            log.error("Toss 승인 API 실패: status={}, body={}",
                    e.getStatusCode(), errorBody);

            // "기존 요청을 처리중입니다" 에러인 경우, 이미 처리 중이므로 Payment 상태 확인
            if (errorBody != null && errorBody.contains("기존 요청을 처리중입니다")) {
                log.warn("Toss에서 '기존 요청을 처리중입니다' 에러 발생. Payment 상태 확인 후 처리");

                // 잠시 대기 후 Payment 상태 재확인
                try {
                    Thread.sleep(1000); // 1초 대기
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                // Payment 상태 재확인
                Payment refreshedPayment = paymentRepository.findByPgOrderId(pgOrderId)
                        .orElseThrow(() -> new ApiException(ErrorCode.INVALID_PAYMENT_REQUEST));

                // 이미 승인되었으면 성공으로 처리
                if (refreshedPayment.getStatus() == PaymentStatus.APPROVED) {
                    log.info("Payment가 이미 승인됨. 성공으로 처리");
                    return; // 이미 처리되었으므로 종료
                }
            }

            payment.abort("TOSS_CONFIRM_FAILED" + errorBody);
            throw new ApiException(ErrorCode.INVALID_PAYMENT_REQUEST);

        } catch (Exception e) {

            payment.abort("TOSS_CONFIRM_FAILED" + e.getMessage());
            throw new RuntimeException(e);

        }

        // 2차 검증
        if (tossRes == null || tossRes.totalAmount() == null || tossRes.totalAmount() != serverAmount) {
            log.error("Toss 응답 검증 실패: tossRes={}, totalAmount={}, expected={}",
                    tossRes, tossRes != null ? tossRes.totalAmount() : "null", serverAmount);
            payment.abort("TOSS_AMOUNT_MISMATCH");
            throw new ApiException(ErrorCode.INVALID_PAYMENT_REQUEST);
        }

        // 승인 반영 후 보관함 저장
        payment.approve(paymentKey);

        ChargeCheck check = ChargeCheck.builder().payment(payment).build();
        chargeCheckRepository.save(check);

    }

    /**
     * 환불:
     *  - 조건: ChargeCheck 테이블에서 수령하지 않고 7일 이내일 때
     */
    @Transactional
    public void cancelPayment(Long userId, String cancelReason, Long chargeCheckId) {

        ChargeCheck check = chargeCheckRepository.findById(chargeCheckId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_PAYMENT_REQUEST));
        User user = userService.getUserByUserId(userId);

        Payment payment = check.getPayment();

        // 유저 같은지 체크
        if (userId != check.getUser().getId()) {
            throw new ApiException(ErrorCode.INVALID_PAYMENT_REQUEST);
        }

        // 수령 완료 상태인지 체크
        if (!check.getStatus().equals(CheckStatus.UNCHECKED)) {
            throw new ApiException(ErrorCode.INVALID_PAYMENT_REQUEST);
        }

        // 일주일 이내인지 체크
        if (check.getCreatedAt().isAfter(check.getCreatedAt().plusDays(7))) {
            throw new ApiException(ErrorCode.INVALID_PAYMENT_REQUEST);
        }

        // payment에서 환불이 이미 이뤄졌는지 체크
        if (payment.getStatus() == PaymentStatus.CANCELED) {
            throw new ApiException(ErrorCode.INVALID_PAYMENT_REQUEST);
        }

        try {
            tossPaymentsClient.cancel(payment.getPaymentKey(), cancelReason);
        } catch (HttpStatusCodeException e) {
            payment.abort("TOSS_CANCEL_FAILED" + e.getMessage());
            throw new ApiException(ErrorCode.INVALID_PAYMENT_REQUEST);
        }

        // 결제 취소 후 로컬 상태 변경
        check.changeStatus(CheckStatus.CANCELED);
        payment.cancel();
    }

    /**
     * 결제 승인 실패
     */
    @Transactional
    public void abortPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_PAYMENT_REQUEST));
        payment.abort("TOSS_ABORT_FAILED" + reason);
    }
}
