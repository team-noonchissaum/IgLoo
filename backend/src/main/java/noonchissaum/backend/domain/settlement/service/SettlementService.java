package noonchissaum.backend.domain.settlement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.order.entity.Order;
import noonchissaum.backend.domain.order.entity.OrderStatus;
import noonchissaum.backend.domain.order.event.OrderConfirmedEvent;
import noonchissaum.backend.domain.order.repository.OrderRepository;
import noonchissaum.backend.domain.settlement.entity.Settlement;
import noonchissaum.backend.domain.settlement.entity.SettlementStatus;
import noonchissaum.backend.domain.settlement.repository.SettlementRepository;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.config.SettlementConfig;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {
    private final SettlementRepository settlementRepository;
    private final OrderRepository orderRepository;
    private final SettlementConfig settlementConfig;
    private final WalletService walletService;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderConfirmed(OrderConfirmedEvent event) {

        Long orderId = event.orderId();
        log.info("정산 시작: orderId={}", orderId);

        try {
            settleOnOrderConfirmed(orderId);
        } catch (Exception e) {
            // 여기 정책은 선택:
            // 1) 로그만 남기고 끝(비동기 재처리 필요)
            // 2) 알림/재시도 큐/스케줄러로 보강
            log.error("Settlement failed after order confirmed. orderId={}", orderId, e);
        }
    }

    @Transactional
    public void settleOnOrderConfirmed(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getBuyer() == null) throw new ApiException(ErrorCode.SETTLEMENT_USER_NOT_FOUND);
        if (order.getStatus() != OrderStatus.COMPLETED) {

            log.error("정산 실패: 주문 상태가 COMPLETED가 아닙니다. 현재 상태: {}, 주문ID: {}", order.getStatus(), orderId);
            throw new ApiException(ErrorCode.INVALID_ORDER_STATUS_FOR_SETTLEMENT);

        }

        if (settlementRepository.existsByOrder_Id(orderId)) {
            throw new ApiException(ErrorCode.SETTLEMENT_ALREADY_EXISTS);
        }

        BigDecimal gross = order.getFinalPrice();
        if (gross == null || gross.compareTo(BigDecimal.ZERO) <= 0)
            throw new ApiException(ErrorCode.INVALID_ORDER_AMOUNT);

        Long buyerId = order.getBuyer().getId();
        Long sellerId = order.getSeller().getId();

        BigDecimal feeRate = settlementConfig.getFeeRate();
        if (feeRate == null) feeRate = new BigDecimal("0.10");

        BigDecimal fee = gross.multiply(feeRate).setScale(0, RoundingMode.DOWN);
        BigDecimal net = gross.subtract(fee);

        walletService.releaseBuyerLockedForOrder(buyerId, gross, orderId);
        walletService.settleToSellerForOrder(sellerId, net, orderId);

        try {
            Settlement settlement = Settlement.builder()
                    .order(order)
                    .buyerId(buyerId)
                    .sellerId(sellerId)
                    .grossAmount(gross)
                    .feeAmount(fee)
                    .netAmount(net)
                    .status(SettlementStatus.COMPLETED)
                    .completedAt(LocalDateTime.now())
                    .build();

            settlementRepository.save(settlement);
        } catch (DataIntegrityViolationException e) {
            log.info("Settlement already created (orderId={})", orderId);
        }
    }
}
