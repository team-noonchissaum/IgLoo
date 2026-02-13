package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.dto.req.AuctionRegisterReq;
import noonchissaum.backend.domain.auction.dto.res.AuctionListRes;
import noonchissaum.backend.domain.auction.dto.res.AuctionRes;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionSortType;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.spec.AuctionSpecs;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.category.service.CategoryService;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.item.service.ItemService;
import noonchissaum.backend.domain.item.service.UserViewRedisLogger;
import noonchissaum.backend.domain.item.service.WishService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final ItemService itemService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final WishService wishService;
    private final AuctionRedisService auctionRedisService;
    private final AuctionRealtimeSnapshotService snapshotService;
    private final AuctionQueryService auctionQueryService;
    private final AuctionIndexService auctionIndexService;
    private final WalletService walletService;
    private final UserViewRedisLogger userViewRedisLogger; // 상세 조회 시 Redis 조회 로그 기록
    private final noonchissaum.backend.recommendation.service.RecommendationService recommendationService; // 추천 서비스 주입


    /**
     * 경매 등록을 처리한다.
     * 먼저 상품(Item)을 생성한 뒤, 해당 상품 기준으로 경매(Auction)를 생성한다.
     */
    @Transactional
    public Long registerAuction(Long userId, AuctionRegisterReq request) {
        User seller = userService.getUserByUserId(userId);
        Category category = categoryService.getCategory(request.getCategoryId());

        // 1. 상품(Item) 생성 및 기본값 세팅
        Item item = itemService.createItem(seller, category, request);

        // 2. 경매(Auction) 생성
        LocalDateTime startAt = request.getStartAt() != null ? request.getStartAt() : LocalDateTime.now();
        LocalDateTime endAt = request.getEndAt() != null ? request.getEndAt()
                : (request.getAuctionDuration() != null ? startAt.plusHours(request.getAuctionDuration()) : startAt.plusHours(1));

        Auction auction = Auction.builder()
                .item(item)
                .startPrice(request.getStartPrice())
                .startAt(startAt)
                .endAt(endAt)
                .build();
        auctionRepository.save(auction);

        int amount = (int) Math.min(auction.getCurrentPrice().longValue() * 0.05, 1000);

        walletService.setAuctionDeposit(userId, auction.getId(), amount, "set");

        auctionRedisService.setRedis(auction.getId());

        // 가격 인덱스(카테고리 기준) 반영
        Long categoryId = category.getId();
        auctionIndexService.updatePriceIndex(auction.getId(), categoryId, auction.getCurrentPrice());

        return auction.getId();
    }

    /**
     * 경매 목록 조회.
     * N+1 문제를 줄이기 위해 Repository에서 fetch 전략을 적용한다.
     */
    public Page<AuctionRes> getAuctionList(Long userId, Pageable pageable, AuctionStatus status) {
        Page<Auction> auctions;
        if (status != null) {
            auctions = auctionRepository.findAllByStatus(status, pageable);
        } else {
            auctions = auctionRepository.findAll(pageable);
        }
        List<Long> itemIds = auctions.getContent().stream()
                .map(a -> a.getItem().getId())
                .distinct()
                .toList();

        Set<Long> wishedItemIds = wishService.getWishedItemIds(userId, itemIds);

        return auctions.map(a ->
                AuctionRes.from(a, wishedItemIds.contains(a.getItem().getId()))
        );
    }


    /**
     * 경매 상세 조회.
     */
    public AuctionRes getAuctionDetail(Long userId, Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_AUCTIONS));
        boolean isWished = wishService.isWished(userId, auction.getItem().getId());

        // 로그인 사용자라면 조회 로그를 Redis에 기록
        if (userId != null) {
            userViewRedisLogger.logView(userId, auction.getItem().getId());
        }

        // 추천 경매 조회
        List<AuctionRes> recommendedAuctions = recommendationService.getRecommendedAuctions(userId, auction.getItem().getId(), auction.getId());

        return AuctionRes.from(auction, isWished, recommendedAuctions);
    }

    /**
     * 경매 취소 처리.
     * 판매자 본인 여부, 입찰 존재 여부, 상태(READY/RUNNING)를 검증한 뒤 보증금 처리 후 취소한다.
     */
    @Transactional
    public void cancelAuction(Long userId, Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_AUCTIONS));

        // 판매자 본인 검증
        if (!auction.getItem().getSeller().getId().equals(userId)) {
            throw new CustomException(ErrorCode.AUCTION_NOT_OWNER);
        }

        // 입찰이 하나라도 있으면 취소 불가
        if (auction.getBidCount() > 0) {
            throw new CustomException(ErrorCode.AUCTION_HAS_BIDS);
        }

        /**
         * 경매 생성 후 5분 이내 취소면 보증금 환불,
         * 5분 초과 취소면 보증금 몰수.
         * 취소 가능한 상태는 READY, RUNNING.
         */
        if (!(auction.getStatus() == AuctionStatus.READY || auction.getStatus() == AuctionStatus.RUNNING)) {
            throw new CustomException(ErrorCode.AUCTION_INVALID_STATUS);
        }

        // 5분 기준 시각 계산
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime penaltyAt = auction.getCreatedAt().plusMinutes(5);

        int amount = (int) Math.min(auction.getCurrentPrice().longValue() * 0.05, 1000);

        if (now.isBefore(penaltyAt)) {
            // 5분 이내 취소: 보증금 환불
            auction.refundDeposit();
            walletService.setAuctionDeposit(userId, auctionId, amount, "refund");
        } else {
            // 5분 초과 취소: 보증금 몰수
            auction.forfeitDeposit();
            walletService.setAuctionDeposit(userId, auctionId, amount, "forfeit");
        }

        // 경매 취소 상태 반영
        auction.cancel();

        // Redis 상태 정리
        auctionRedisService.cancelAuction(auctionId);
    }

    /**
     * 내 경매 목록 조회.
     */
    public Page<AuctionRes> getMyAuctions(Long userId, Pageable pageable) {
        Page<Auction> auctions = auctionRepository.findAllByItem_Seller_Id(userId, pageable);
        return auctions.map(AuctionRes::from);
    }

    @Transactional(readOnly = true)
    public Page<AuctionListRes> searchAuctionList(
            Long userId,
            AuctionStatus status,
            Long categoryId,
            String keyword,
            AuctionSortType sort,
            int page,
            int size
    ) {
        // PRICE 정렬은 Redis ZSET 인덱스 기반 QueryService 사용
        if (sort != null && sort.isRedisPriceSort()) {
            // categoryId 없으면 조회 불가
            if (categoryId == null) {
                return new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
            }
            // category + price 정렬은 status/keyword 조합 미지원
            if (status != null || (keyword != null && !keyword.isBlank())) {
                // 미지원 조합은 빈 결과 반환
                return new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
            }

            return auctionQueryService.searchByCategoryPriceSorted(userId, categoryId, sort, page, size);
        }

        Pageable pageable = PageRequest.of(page, size, sort == null ? Sort.by(Sort.Direction.DESC,
                "startAt") : sort.toSort());

        var spec = AuctionSpecs.filter(status, categoryId, keyword);

        Page<Auction> auctions = auctionRepository.findAll(spec, pageable);

        // 찜 여부 계산 대상 itemId 수집
        List<Long> itemIds = auctions.getContent().stream()
                .map(a -> a.getItem().getId())
                .distinct()
                .toList();

        Set<Long> wishedItemIds = wishService.getWishedItemIds(userId, itemIds);

        return auctions.map(a -> {
            boolean isWished = wishedItemIds.contains(a.getItem().getId());
            AuctionListRes base = AuctionListRes.from(a, isWished);

            return snapshotService.getSnapshotIfPresent(a.getId())
                    .map(snap -> base.withRealtime(
                            snap.getCurrentPrice(),
                            snap.getBidCount(),
                            snap.getEndAt()
                    ))
                    .orElse(base);
        });
    }
}
