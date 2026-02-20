package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.dto.ws.AuctionExtendedPayload;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionRecordService {

    private final AuctionRepository auctionRepository;
    private final UserService userService;
    private final AuctionRedisService auctionRedisService;
    private final AuctionIndexService auctionIndexService;

    private final AuctionMessageService auctionMessageService;

    @Transactional
    public void saveAuction(Long auctionId, Long userId, BigDecimal bidAmount) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_AUCTIONS));
        User user = userService.getUserByUserId(userId);

        auction.updateBid(user, bidAmount);
        // Redis 스냅샷 갱신
        auctionRedisService.setRedis(auctionId);

        // Redis ZSET 가격 인덱스 갱신 (카테고리+가격정렬 )
        Long categoryId = auction.getItem().getCategory().getId();
        auctionIndexService.updatePriceIndex(auctionId, categoryId, auction.getCurrentPrice());
    }

    /**
     * 입찰 업데이트와 시간 연장 로직을 한 트랜잭션으로 처리하여 데이터 유실 방지
     */
    @Transactional
    public void updateAuctionWithExtension(Long auctionId, Long userId, BigDecimal bidAmount) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_AUCTIONS));
        User user = userService.getUserByUserId(userId);

        // 1. 입찰 정보 업데이트
        auction.updateBid(user, bidAmount);

        // 2. 시간 연장 로직 실행
        boolean extended = auction.extendIfNeeded(LocalDateTime.now());

        // 3. Redis 동기화 (상태가 변한 후 한 번만 호출)
        auctionRedisService.setRedis(auctionId);

        // 4. 연장된 경우 웹소켓 알림 발송
        if (extended) {
            auctionMessageService.sendAuctionExtended(auctionId, 
                AuctionExtendedPayload.builder()
                    .auctionId(auctionId)
                    .endAt(auction.getEndAt())
                    .isExtended(true)
                    .extendedMinutes(3)
                    .build());
        }

        // 5. 가격 인덱스 갱신
        Long categoryId = auction.getItem().getCategory().getId();
        auctionIndexService.updatePriceIndex(auctionId, categoryId, auction.getCurrentPrice());
    }
}
