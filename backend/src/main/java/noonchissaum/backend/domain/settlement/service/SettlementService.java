package noonchissaum.backend.domain.settlement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.order.entity.Order;
import noonchissaum.backend.domain.order.entity.OrderStatus;
import noonchissaum.backend.domain.order.service.OrderService;
import noonchissaum.backend.domain.settlement.entity.Settlement;
import noonchissaum.backend.domain.settlement.entity.SettlementStatus;
import noonchissaum.backend.domain.settlement.repository.SettlementRepository;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.config.SettlementConfig;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {
    private final SettlementRepository settlementRepository;
    private final OrderService orderService;
    private final SettlementConfig settlementConfig;
    private final WalletService walletService;

    @Transactional
    public void settleOnOrderConfirmed(Long orderId){
        Order order = orderService.getOrder(orderId);

        // 1) 구매자만 정산
        if (order.getBuyer() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        Long buyerId = order.getBuyer().getId();

        // 2) 구매확정 상태인지 검증
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new ApiException(ErrorCode.INVALID_ORDER_STATUS_FOR_SETTLEMENT);
        }

        // 3) 이미 정산됐으면 그냥 리턴
        if (settlementRepository.existsByOrder_Id(orderId)) {
            return;
        }

        BigDecimal gross = order.getFinalPrice();
        if (gross == null || gross.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ErrorCode.INVALID_ORDER_AMOUNT);
        }

        Long sellerId = order.getSeller().getId();
        Long systemUserId = settlementConfig.getSystemUserId();

        BigDecimal feeRate = settlementConfig.getFeeRate();
        if (feeRate == null) feeRate = new BigDecimal("0.10");

        // 수수료
        BigDecimal fee = gross.multiply(feeRate).setScale(0, RoundingMode.DOWN);
        BigDecimal net = gross.subtract(fee);

        // 구매자 locked 차감
        walletService.releaseBuyerLockedForOrder(buyerId, gross, orderId);

        // 판매자 정산액 지급
        walletService.settleToSellerForOrder(sellerId, net, orderId);

        // 플랫폼 수수료 적립
        walletService.settleFeeToPlatform(systemUserId, fee, orderId);

        // Settlement 기록
        try {
            Settlement settlement = Settlement.builder()
                    .order(order)
                    .buyerId(buyerId)
                    .sellerId(sellerId)
                    .platformUserId(systemUserId)
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
