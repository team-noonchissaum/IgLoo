package noonchissaum.backend.domain.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.order.dto.payment.res.PaymentPrepareRes;
import noonchissaum.backend.domain.order.dto.payment.res.VirtualAccountInfo;
import noonchissaum.backend.domain.order.entity.*;
import noonchissaum.backend.domain.order.repository.ChargeCheckRepository;
import noonchissaum.backend.domain.order.repository.PaymentRepository;
import noonchissaum.backend.domain.toss.TossPaymentsClient;
import noonchissaum.backend.domain.toss.dto.confirm.TossConfirmRes;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import java.time.LocalDateTime;
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
    public VirtualAccountInfo confirmPayment(String pgOrderId, String paymentKey, Integer amount) {
        log.info("결제 승인 시작: pgOrderId={}, paymentKey={}", pgOrderId, paymentKey);

        Payment payment = paymentRepository.findByPgOrderId(pgOrderId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_PAYMENT));

        // 멱등성: REQUEST 상태 체크
        if (payment.getStatus() != PaymentStatus.REQUEST) {
            throw new ApiException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        // 멱등성
        if (chargeCheckRepository.existsByPaymentId(payment.getId())) {
            log.info("이미 charge 생성된 결제: paymentId={}", payment.getId());
            throw new ApiException(ErrorCode.ALREADY_EXISTS_PAYMENT);
        }

        // amount 체크 변조 방지
        int serverAmount = payment.getAmount().intValue();
        if (amount == null || amount != serverAmount) {
            throw new ApiException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        // 체크 완료 후 승인 api 호출
        TossConfirmRes tossRes;
        try {

            tossRes = tossPaymentsClient.confirm(paymentKey, pgOrderId, amount);

            if ("가상계좌".equals(tossRes.method())) {
                // 1. 가상계좌: 입금 대기 상태로 변경하고 계좌 정보를 저장합니다.
                // ChargeCheck(포인트 적립 등)는 생성하지 않습니다.
                payment.waitDeposit(paymentKey,
                        tossRes.virtualAccount().bank(),
                        tossRes.virtualAccount().accountNumber(),
                        tossRes.virtualAccount().dueDate());
                log.info("가상계좌 발급 완료: 입금 대기 상태 pgOrderId={}", pgOrderId);
                return VirtualAccountInfo.from(tossRes.virtualAccount());
            }

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
                        .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_PAYMENT));

                // 이미 승인되었으면 성공으로 처리
                if (refreshedPayment.getStatus() == PaymentStatus.APPROVED) {
                    log.info("Payment가 이미 승인됨. 성공으로 처리");
                    return null;
                }
            }

            payment.abort("TOSS_CONFIRM_FAILED" + errorBody);
            log.error(e.getMessage());
            throw new ApiException(ErrorCode.PAYMENTS_FAILED);

        } catch (Exception e) {

            payment.abort("TOSS_CONFIRM_FAILED" + e.getMessage());
            throw new ApiException(ErrorCode.PAYMENTS_FAILED);

        }

        // 2차 검증
        if (tossRes == null || tossRes.totalAmount() == null || tossRes.totalAmount() != serverAmount) {
            log.error("Toss 응답 검증 실패: tossRes={}, totalAmount={}, expected={}",
                    tossRes, tossRes != null ? tossRes.totalAmount() : "null", serverAmount);
            payment.abort("TOSS_AMOUNT_MISMATCH");
            throw new ApiException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        // 승인 반영 후 보관함 저장
        payment.approve(paymentKey);

        ChargeCheck check = ChargeCheck.builder().payment(payment).build();
        chargeCheckRepository.save(check);
        return null;
    }

    /**
     * 환불:
     *  - 조건: ChargeCheck 테이블에서 수령하지 않고 7일 이내일 때
     */
    @Transactional
    public void cancelPayment(Long userId, String cancelReason, Long chargeCheckId) {

        ChargeCheck check = chargeCheckRepository.findById(chargeCheckId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHARGE_CHECK_NOT_FOUND));
        User user = userService.getUserByUserId(userId);

        Payment payment = check.getPayment();

        // 유저 같은지 체크
        if (userId != check.getUser().getId()) {
            throw new ApiException(ErrorCode.REFUND_ACCESS_DENIED);
        }

        // 수령 완료 상태인지 체크
        if (!check.getStatus().equals(CheckStatus.UNCHECKED)) {
            throw new ApiException(ErrorCode.CHARGE_CONFIRMED);
        }

        // 일주일 이내인지 체크
        if (check.getCreatedAt().plusDays(7).isBefore(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.REFUND_DATE_EXPIRED);
        }

        // payment에서 환불이 이미 이뤄졌는지 체크
        if (payment.getStatus() == PaymentStatus.CANCELED) {
            throw new ApiException(ErrorCode.ALREADY_CANCELED_PAYMENT);
        }

        try {
            tossPaymentsClient.cancel(payment.getPaymentKey(), cancelReason);
        } catch (HttpStatusCodeException e) {
            payment.abort("TOSS_CANCEL_FAILED" + e.getMessage());
            throw new ApiException(ErrorCode.REFUND_FAILED);
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
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_PAYMENT));
        payment.abort("TOSS_ABORT_FAILED" + reason);
    }

    /**
     * 입금 완료 웹훅을 받은 뒤 충전 처리
     * */
    @Transactional
    public void processDepositDone(String paymentKey, String orderId, Long amount) {
        log.info("가상계좌 웹훅 처리 시작: pgOrderId={}, amount={}", orderId, amount);

        // 비관적 락을 통해 연속된 요청이 오면 대기
        Payment payment = paymentRepository.findByPgOrderIdWithLock(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_PAYMENT));

        // 멱등성 체크
        if (payment.getStatus() != PaymentStatus.WAITING_FOR_DEPOSIT) {
            log.warn("이미 처리되었거나 유효하지 않은 상태의 웹훅 요청: status={}, pgOrderId={}",
                    payment.getStatus(), orderId);
            throw new ApiException(ErrorCode.WEBHOOK_INVALID_STATE);
        }

        // 결제 금액 확인
        if (amount != payment.getAmount().longValue()) {
            log.error("결제 금액 불일치: DB={}, Webhook={}", payment.getAmount(), amount);
            throw new ApiException(ErrorCode.WEBHOOK_AMOUNT_MISMATCH);
        }

        payment.approve(paymentKey);
        ChargeCheck check = ChargeCheck.builder().payment(payment).build();
        chargeCheckRepository.save(check);
        log.info("가상계좌 입금 확인 및 충전 완료: paymentId={}", payment.getId());
    }
}
