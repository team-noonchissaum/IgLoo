package noonchissaum.backend.domain.settlement.service;

import noonchissaum.backend.domain.order.entity.Order;
import noonchissaum.backend.domain.order.entity.OrderStatus;
import noonchissaum.backend.domain.order.event.OrderConfirmedEvent;
import noonchissaum.backend.domain.order.repository.OrderRepository;
import noonchissaum.backend.domain.settlement.entity.Settlement;
import noonchissaum.backend.domain.settlement.entity.SettlementStatus;
import noonchissaum.backend.domain.settlement.repository.SettlementRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.config.SettlementConfig;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SettlementConfig settlementConfig;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private SettlementService settlementService;

    @Test
    @DisplayName("주문이 없으면 ORDER_NOT_FOUND 예외가 발생한다")
    void settleOnOrderConfirmed_throwsWhenOrderNotFound() {
        Long orderId = 1L;
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> settlementService.settleOnOrderConfirmed(orderId))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 상태가 COMPLETED가 아니면 INVALID_ORDER_STATUS_FOR_SETTLEMENT 예외")
    void settleOnOrderConfirmed_throwsWhenOrderStatusIsNotCompleted() {
        Long orderId = 2L;
        Order order = order(orderId, OrderStatus.PAID, BigDecimal.valueOf(10000));
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> settlementService.settleOnOrderConfirmed(orderId))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_STATUS_FOR_SETTLEMENT);
    }

    @Test
    @DisplayName("구매자 정보가 없으면 USER_NOT_FOUND 예외")
    void settleOnOrderConfirmed_throwsWhenBuyerIsNull() {
        Long orderId = 21L;
        User seller = user(202L, "seller@test.com", "seller");

        Order order = Order.builder()
                .buyer(null)
                .seller(seller)
                .status(OrderStatus.COMPLETED)
                .finalPrice(BigDecimal.valueOf(10000))
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> settlementService.settleOnOrderConfirmed(orderId))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 정산이 존재하면 지갑 처리 없이 종료한다")
    void settleOnOrderConfirmed_returnsWhenSettlementAlreadyExists() {
        Long orderId = 3L;
        Order order = order(orderId, OrderStatus.COMPLETED, BigDecimal.valueOf(10000));

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(settlementRepository.existsByOrder_Id(orderId)).willReturn(true);

        settlementService.settleOnOrderConfirmed(orderId);

        verify(walletService, never()).releaseBuyerLockedForOrder(any(), any(), any());
        verify(walletService, never()).settleToSellerForOrder(any(), any(), any());
        verify(walletService, never()).settleFeeToPlatform(any(), any(), any());
        verify(settlementRepository, never()).save(any(Settlement.class));
    }

    @Test
    @DisplayName("정상 정산 시 fee/net 계산 후 지갑 반영 및 COMPLETED 정산내역 저장")
    void settleOnOrderConfirmed_settlesAndSavesCompletedSettlement() {
        Long orderId = 4L;
        Long buyerId = 10L;
        Long sellerId = 20L;
        Long platformId = 10000L;

        Order order = order(orderId, OrderStatus.COMPLETED, BigDecimal.valueOf(12345));
        ReflectionTestUtils.setField(order.getBuyer(), "id", buyerId);
        ReflectionTestUtils.setField(order.getSeller(), "id", sellerId);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(settlementRepository.existsByOrder_Id(orderId)).willReturn(false);
        given(settlementConfig.getSystemUserId()).willReturn(platformId);
        given(settlementConfig.getFeeRate()).willReturn(new BigDecimal("0.10"));

        settlementService.settleOnOrderConfirmed(orderId);

        // fee = 12345 * 0.10 = 1234.5 -> DOWN => 1234, net => 11111
        verify(walletService).releaseBuyerLockedForOrder(buyerId, BigDecimal.valueOf(12345), orderId);
        verify(walletService).settleToSellerForOrder(sellerId, BigDecimal.valueOf(11111), orderId);
        verify(walletService).settleFeeToPlatform(platformId, BigDecimal.valueOf(1234), orderId);

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(captor.capture());

        Settlement saved = captor.getValue();
        assertThat(saved.getOrder()).isEqualTo(order);
        assertThat(saved.getBuyerId()).isEqualTo(buyerId);
        assertThat(saved.getSellerId()).isEqualTo(sellerId);
        assertThat(saved.getPlatformUserId()).isEqualTo(platformId);
        assertThat(saved.getGrossAmount()).isEqualTo(BigDecimal.valueOf(12345));
        assertThat(saved.getFeeAmount()).isEqualTo(BigDecimal.valueOf(1234));
        assertThat(saved.getNetAmount()).isEqualTo(BigDecimal.valueOf(11111));
        assertThat(saved.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(saved.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("주문 금액이 0 이하이면 INVALID_ORDER_AMOUNT 예외")
    void settleOnOrderConfirmed_throwsWhenGrossAmountIsInvalid() {
        Long orderId = 5L;
        Order order = order(orderId, OrderStatus.COMPLETED, BigDecimal.ZERO);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(settlementRepository.existsByOrder_Id(orderId)).willReturn(false);

        assertThatThrownBy(() -> settlementService.settleOnOrderConfirmed(orderId))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_AMOUNT);

        verify(walletService, never()).releaseBuyerLockedForOrder(any(), any(), any());
    }

    @Test
    @DisplayName("수수료율 설정이 null이면 기본 10%를 적용한다")
    void settleOnOrderConfirmed_usesDefaultFeeRateWhenConfigIsNull() {
        Long orderId = 6L;
        Long platformId = 10000L;
        Order order = order(orderId, OrderStatus.COMPLETED, BigDecimal.valueOf(20000));

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(settlementRepository.existsByOrder_Id(orderId)).willReturn(false);
        given(settlementConfig.getSystemUserId()).willReturn(platformId);
        given(settlementConfig.getFeeRate()).willReturn(null);

        settlementService.settleOnOrderConfirmed(orderId);

        verify(walletService).settleFeeToPlatform(eq(platformId), eq(BigDecimal.valueOf(2000)), eq(orderId));
        verify(walletService).settleToSellerForOrder(eq(order.getSeller().getId()), eq(BigDecimal.valueOf(18000)), eq(orderId));
    }

    @Test
    @DisplayName("정산 저장 시 유니크 충돌이 나면 예외를 던지지 않고 종료한다")
    void settleOnOrderConfirmed_ignoresDuplicateSaveException() {
        Long orderId = 7L;
        Long platformId = 10000L;
        Order order = order(orderId, OrderStatus.COMPLETED, BigDecimal.valueOf(10000));

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(settlementRepository.existsByOrder_Id(orderId)).willReturn(false);
        given(settlementConfig.getSystemUserId()).willReturn(platformId);
        given(settlementConfig.getFeeRate()).willReturn(new BigDecimal("0.10"));
        given(settlementRepository.save(any(Settlement.class))).willThrow(new DataIntegrityViolationException("duplicate"));

        settlementService.settleOnOrderConfirmed(orderId);

        verify(walletService).releaseBuyerLockedForOrder(order.getBuyer().getId(), BigDecimal.valueOf(10000), orderId);
        verify(walletService).settleToSellerForOrder(order.getSeller().getId(), BigDecimal.valueOf(9000), orderId);
        verify(walletService).settleFeeToPlatform(platformId, BigDecimal.valueOf(1000), orderId);
    }

    @Test
    @DisplayName("이벤트 리스너는 정산 중 예외가 나도 외부로 전파하지 않는다")
    void onOrderConfirmed_doesNotThrowWhenSettlementFails() {
        Long orderId = 8L;
        SettlementService spyService = spy(settlementService);
        doThrow(new RuntimeException("boom")).when(spyService).settleOnOrderConfirmed(orderId);

        spyService.onOrderConfirmed(new OrderConfirmedEvent(orderId));

        verify(spyService).settleOnOrderConfirmed(orderId);
    }

    private Order order(Long orderId, OrderStatus status, BigDecimal finalPrice) {
        User buyer = user(101L, "buyer@test.com", "buyer");
        User seller = user(202L, "seller@test.com", "seller");

        Order order = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .status(status)
                .finalPrice(finalPrice)
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }

    private User user(Long id, String email, String nickname) {
        User user = User.builder()
                .email(email)
                .nickname(nickname)
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
