package noonchissaum.backend.domain.order.service.unit;

import noonchissaum.backend.domain.order.dto.payment.res.VirtualAccountInfo;
import noonchissaum.backend.domain.order.entity.ChargeCheck;
import noonchissaum.backend.domain.order.entity.Payment;
import noonchissaum.backend.domain.order.entity.PaymentStatus;
import noonchissaum.backend.domain.order.entity.PgProvider;
import noonchissaum.backend.domain.order.repository.ChargeCheckRepository;
import noonchissaum.backend.domain.order.repository.PaymentRepository;
import noonchissaum.backend.domain.order.service.PaymentService;
import noonchissaum.backend.domain.toss.TossPaymentsClient;
import noonchissaum.backend.domain.toss.dto.confirm.TossConfirmRes;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PaymentServiceUnitTest {

    @Mock
    private TossPaymentsClient tossPaymentsClient;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ChargeCheckRepository chargeCheckRepository;
    @Mock
    private UserService userService;

    @Test
    @DisplayName("결제 준비 금액이 0 이하이면 INVALID_PAYMENT_REQUEST 예외")
    void preparePayment_withInvalidAmount_throwsApiException() {
        PaymentService service = new PaymentService(tossPaymentsClient, paymentRepository, chargeCheckRepository, userService);
        when(userService.getUserByUserId(1L)).thenReturn(sampleUser(1L));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.preparePayment(1L, 0, PgProvider.TOSS));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_PAYMENT_REQUEST);
    }

    @Test
    @DisplayName("가상계좌 결제 승인 시 입금대기 상태로 변경하고 계좌정보 반환")
    void confirmPayment_virtualAccount_returnsVirtualAccountInfo() {
        PaymentService service = new PaymentService(tossPaymentsClient, paymentRepository, chargeCheckRepository, userService);
        Payment payment = Payment.builder()
                .user(sampleUser(2L))
                .amount(BigDecimal.valueOf(50000))
                .pgProvider(PgProvider.TOSS)
                .pgOrderId("pg-order-1")
                .build();
        ReflectionTestUtils.setField(payment, "id", 100L);

        when(paymentRepository.findByPgOrderId("pg-order-1")).thenReturn(Optional.of(payment));
        when(chargeCheckRepository.existsByPaymentId(100L)).thenReturn(false);
        when(tossPaymentsClient.confirm("payment-key", "pg-order-1", 50000))
                .thenReturn(new TossConfirmRes(
                        "payment-key",
                        "pg-order-1",
                        "DONE",
                        50000,
                        "2026-01-01T00:00:00+09:00",
                        "가상계좌",
                        null,
                        new TossConfirmRes.VirtualAccount("1234567890", "신한", "테스터", "2026-01-15"),
                        null
                ));

        VirtualAccountInfo result = service.confirmPayment("pg-order-1", "payment-key", 50000);

        assertThat(result).isNotNull();
        assertThat(result.bank()).isEqualTo("신한");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.WAITING_FOR_DEPOSIT);
        verify(chargeCheckRepository, never()).save(org.mockito.ArgumentMatchers.any(ChargeCheck.class));
    }

    @Test
    @DisplayName("입금 완료 웹훅 금액이 다르면 승인/충전을 진행하지 않음")
    void processDepositDone_whenAmountMismatch_doesNotApprove() {
        PaymentService service = new PaymentService(tossPaymentsClient, paymentRepository, chargeCheckRepository, userService);
        Payment payment = Payment.builder()
                .user(sampleUser(3L))
                .amount(BigDecimal.valueOf(10000))
                .pgProvider(PgProvider.TOSS)
                .pgOrderId("pg-order-2")
                .build();
        ReflectionTestUtils.setField(payment, "status", PaymentStatus.WAITING_FOR_DEPOSIT);

        when(paymentRepository.findByPgOrderIdWithLock("pg-order-2")).thenReturn(Optional.of(payment));

        service.processDepositDone("pk-2", "pg-order-2", 9999L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.WAITING_FOR_DEPOSIT);
        verify(chargeCheckRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private User sampleUser(Long userId) {
        User user = User.builder()
                .email("payment-service-" + userId + "@test.com")
                .nickname("payment_service_" + userId)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
