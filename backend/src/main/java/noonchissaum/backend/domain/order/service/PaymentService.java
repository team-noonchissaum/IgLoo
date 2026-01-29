package noonchissaum.backend.domain.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.order.dto.payment.res.PaymentPrepareRes;
import noonchissaum.backend.domain.order.entity.Payment;
import noonchissaum.backend.domain.order.entity.PaymentStatus;
import noonchissaum.backend.domain.order.entity.PgProvider;
import noonchissaum.backend.domain.order.repositroy.ChargeCheckRepository;
import noonchissaum.backend.domain.order.repositroy.PaymentRepository;
import noonchissaum.backend.domain.toss.TossPaymentsClient;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public PaymentPrepareRes preparePayment(Long userId, BigDecimal amount, PgProvider pgProvider) {

        User user = userService.getUserByUserId(userId);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ErrorCode.INVALID_PAYMENT_REQUEST);
        }

        String pgOrderId = generatePgOrderId();

        Payment payment = Payment.builder()
                .pgProvider(pgProvider)
                .amount(amount)
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
    public void confirmPayment(String pgOrderId, String paymentKey, BigDecimal amount) {
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

        // amount 체크
        if (amount == null || amount.compareTo())
    }
}
