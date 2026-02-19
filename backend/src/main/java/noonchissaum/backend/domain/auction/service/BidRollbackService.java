package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.entity.Bid;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.wallet.service.WalletRecordService;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.util.UserLockExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 차단 유저가 최상위 입찰자인 경매를 해당 유저 최초 입찰 전으로 롤백
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BidRollbackService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AuctionRedisService auctionRedisService;
    private final AuctionIndexService auctionIndexService;
    private final AuctionMessageService auctionMessageService;
    private final WalletService walletService;
    private final WalletRecordService walletRecordService;
    private final UserLockExecutor userLockExecutor;

    /**
     * 차단 유저가 최상위 입찰자인 경매들을 롤백
     */
    @Transactional
    public void rollbackAuctionsForBlockedUser(Long blockedUserId) {
        List<Auction> auctions = auctionRepository.findByCurrentBidder_IdAndStatusIn(
                blockedUserId,
                List.of(AuctionStatus.RUNNING, AuctionStatus.DEADLINE)
        );

        for (Auction auction : auctions) {
            try {
                rollbackAuctionIfHasCompetingBids(auction, blockedUserId);
            } catch (Exception e) {
                log.error("경매 {} 롤백 실패 - userId: {}", auction.getId(), blockedUserId, e);
                throw e; // 롤백 실패 시 트랜잭션 롤백
            }
        }
    }

    private void rollbackAuctionIfHasCompetingBids(Auction auction, Long blockedUserId) {
        List<Bid> bids = bidRepository.findByAuctionIdOrderByCreatedAtAsc(auction.getId());

        // 경쟁 입찰 여부: blockedUserId 외 다른 bidder가 있는지
        boolean hasCompetingBids = bids.stream()
                .anyMatch(b -> !b.getBidder().getId().equals(blockedUserId));

        if (!hasCompetingBids) {
            return; // 롤백 대상 아님
        }

        // blocked 유저의 첫 입찰 인덱스
        int firstBlockedBidIndex = -1;
        for (int i = 0; i < bids.size(); i++) {
            if (bids.get(i).getBidder().getId().equals(blockedUserId)) {
                firstBlockedBidIndex = i;
                break;
            }
        }
        if (firstBlockedBidIndex < 0) {
            return;
        }

        User previousBidder;
        BigDecimal rollbackPrice;
        int rollbackBidCount;
        Long previousBidderId;

        if (firstBlockedBidIndex == 0) {
            // 차단 유저가 첫 입찰자 → 등록가(무입찰) 상태로 롤백
            previousBidder = null;
            rollbackPrice = auction.getStartPrice();
            rollbackBidCount = 0;
            previousBidderId = -1L;
        } else {
            // 롤백 대상: firstBlockedBidIndex 직전 입찰
            Bid rollbackTargetBid = bids.get(firstBlockedBidIndex - 1);
            previousBidder = rollbackTargetBid.getBidder();
            rollbackPrice = rollbackTargetBid.getBidPrice();
            rollbackBidCount = firstBlockedBidIndex;
            previousBidderId = previousBidder.getId();
        }

        BigDecimal blockedUserLockedAmount = auction.getCurrentPrice(); // 차단 유저가 잠근 금액

        // 1. DB: Bid 삭제 (차단 유저 입찰만)
        bidRepository.deleteByAuctionIdAndBidderId(auction.getId(), blockedUserId);

        // 2. DB: Auction 상태 롤백
        auction.rollbackBid(previousBidder, rollbackPrice, rollbackBidCount);
        auctionRepository.save(auction);

        // 3. Redis: 경매 상태 갱신
        auctionRedisService.setRedis(auction.getId());

        // 4. Wallet: Redis + DB 역처리 (유저 락으로 동시성 제어)
        List<Long> lockUserIds = new java.util.ArrayList<>(List.of(blockedUserId));
        if (previousBidderId != null && previousBidderId != -1L) {
            lockUserIds.add(previousBidderId);
        }
        lockUserIds.sort(Long::compareTo);

        userLockExecutor.withUserLocks(lockUserIds, () -> {
            // DB 먼저 (트랜잭션 내), 실패 시 Redis 미갱신
            walletRecordService.rollbackWalletRecord(
                    blockedUserId, previousBidderId,
                    blockedUserLockedAmount, rollbackPrice,
                    auction.getId()
            );
            walletService.rollbackBidWallet(
                    blockedUserId, previousBidderId,
                    blockedUserLockedAmount, rollbackPrice
            );
        });

        // 5. 캐시 무효화
        walletService.clearWalletCache(blockedUserId);
        if (previousBidderId != null && previousBidderId != -1L) {
            walletService.clearWalletCache(previousBidderId);
        }

        // 6. Redis ZSET 가격 인덱스 갱신
        Long categoryId = auction.getItem().getCategory().getId();
        auctionIndexService.updatePriceIndex(auction.getId(), categoryId, rollbackPrice);

        // 7. 실시간 메시지: 입찰 롤백 알림 (갱신된 상태로 브로드캐스트)
        auctionMessageService.sendBidSucceeded(auction.getId(),
                noonchissaum.backend.domain.auction.dto.ws.BidSucceededPayload.builder()
                        .auctionId(auction.getId())
                        .currentPrice(rollbackPrice.longValue())
                        .currentBidderId(previousBidder != null ? previousBidder.getId() : null)
                        .bidCount(rollbackBidCount)
                        .endAt(auction.getEndAt())
                        .isExtended(auction.getIsExtended())
                        .build());

        log.info("경매 {} 롤백 완료 - blockedUserId: {}, rollbackPrice: {}, rollbackBidCount: {}",
                auction.getId(), blockedUserId, rollbackPrice, rollbackBidCount);
    }
}
