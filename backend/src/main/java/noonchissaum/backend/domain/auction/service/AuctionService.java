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
import noonchissaum.backend.domain.item.service.WishService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.RedisKeys;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final StringRedisTemplate redisTemplate;
    private final AuctionRedisService auctionRedisService;
    private final AuctionRealtimeSnapshotService snapshotService;
    private final AuctionQueryService auctionQueryService;
    private final AuctionIndexService auctionIndexService;
    private final WalletService walletService;


    /**
     * 새로운 경매를 등록합니다.
     * 상품 정보(Item)를 먼저 생성하고, 관련 이미지와 경매 일정(Auction)을 하나의 트랜잭션으로 묶어 저장합니다.
     */
    @Transactional
    public Long registerAuction(Long userId, AuctionRegisterReq request) {
        User seller = userService.getUserByUserId(userId);
        Category category = categoryService.getcategory(request.getCategoryId());

        // 1. 상품(Item) 정보 생성 + 이미지 등록
        Item item = itemService.createItem(seller,category,request);

        // 3. 경매(Auction) 설정 및 저장
        LocalDateTime startAt = request.getStartAt() != null ? request.getStartAt() : LocalDateTime.now();
        LocalDateTime endAt = request.getEndAt() != null ? request.getEndAt() : 
                (request.getAuctionDuration() != null ? startAt.plusHours(request.getAuctionDuration()) : startAt.plusHours(1));

        Auction auction = Auction.builder()
                .item(item)
                .startPrice(request.getStartPrice())
                .startAt(startAt)
                .endAt(endAt)
                .build();
        auctionRepository.save(auction);

        int amount = (int) Math.min( auction.getCurrentPrice().longValue() * 0.05 , 1000);

        walletService.setAuctionDeposit(userId, auction.getId(), amount, "set");

        auctionRedisService.setRedis(auction.getId());

        // price index 초기값 세팅
        Long categoryId = category.getId();
        auctionIndexService.updatePriceIndex(auction.getId(), categoryId, auction.getCurrentPrice());

        return auction.getId();
    }

    /**
     * 경매 목록을 조회합니다. N+1 문제를 방지하기 위해 Fetch Join이 적용된 Repository 메서드를 사용합니다.
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
     * 경매 상세 정보를 조회합니다.
     */
    public AuctionRes getAuctionDetail(Long userId, Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_AUCTIONS));
        boolean isWished = wishService.isWished(userId, auction.getItem().getId());
        return AuctionRes.from(auction, isWished);
    }

    /**
     * 경매를 취소합니다.
     * 판매자 본인 확인과 입찰자 존재 여부를 검증하여 경매 무결성을 유지합니다。
     */
    @Transactional
    public void cancelAuction(Long userId, Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_AUCTIONS));

        // 판매자 본인 여부 확인
        if (!auction.getItem().getSeller().getId().equals(userId)) {
            throw new CustomException(ErrorCode.AUCTION_NOT_OWNER);
        }

        // 이미 입찰이 진행된 경우 취소 불가 (비즈니스 규칙)
        if (auction.getBidCount() > 0) {
            throw new CustomException(ErrorCode.AUCTION_HAS_BIDS);
        }

        /**
         * 5분이 지났을 경우 보증금 회수하는 로직과 5분이전이면 보증금 돌려주는 로직 추가
         * 5분 이후 취소도 처리하려면 RUNNING 상태도 취소 허용
         */
        if (!(auction.getStatus() == AuctionStatus.READY || auction.getStatus() == AuctionStatus.RUNNING)) {
            throw new CustomException(ErrorCode.AUCTION_INVALID_STATUS);
        }

        // 정책 기준: 등록 + 5분
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime penaltyAt = auction.getCreatedAt().plusMinutes(5);

        int amount = (int) Math.min( auction.getCurrentPrice().longValue() * 0.05 , 1000);

        if (now.isBefore(penaltyAt)) {
            // 5분 이내 취소 → 보증금 환불 처리
            auction.refundDeposit();

             walletService.setAuctionDeposit(userId, auctionId, amount, "refund");
        } else {
            // 5분 이후 취소 → 보증금 몰수 확정(패널티)
            // 5분 이후 취소 -> 보증금 환불 없음
            auction.forfeitDeposit();
            walletService.setAuctionDeposit(userId, auctionId, amount, "forfeit");
        }


        //경매 취소시 상태 변경
        auction.cancel();

        //radis에서 삭제
        auctionRedisService.cancelAuction(auctionId);
    }

    public void checkDeadLine(Long auctionId) {
        String rawEndTime = redisTemplate.opsForValue().get(RedisKeys.auctionEndTime(auctionId));
        String rawImminentMinutes = redisTemplate.opsForValue().get(RedisKeys.auctionImminentMinutes(auctionId));
        String isExtended = redisTemplate.opsForValue().get(RedisKeys.auctionIsExtended(auctionId));

        if (rawEndTime == null || rawImminentMinutes == null || isExtended == null) {
            auctionRedisService.setRedis(auctionId);

            rawEndTime = redisTemplate.opsForValue().get(RedisKeys.auctionEndTime(auctionId));
            rawImminentMinutes = redisTemplate.opsForValue().get(RedisKeys.auctionImminentMinutes(auctionId));
            isExtended = redisTemplate.opsForValue().get(RedisKeys.auctionIsExtended(auctionId));
        }

        LocalDateTime endTime = LocalDateTime.parse(rawEndTime);
        Integer imminentMinutes = Integer.parseInt(rawImminentMinutes);

        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(endTime) || now.isBefore(endTime.minusMinutes(imminentMinutes))) {
            return;
        }

        if (isExtended.equals("true")) {
            return;
        }

        endTime = endTime.plusMinutes(3);

        redisTemplate.opsForValue().set(RedisKeys.auctionEndTime(auctionId), endTime.toString());
        redisTemplate.opsForValue().set(RedisKeys.auctionIsExtended(auctionId), "true");
    }

    /**
     * 내가 등록한 경매 목록 조회
     */
    public Page<AuctionRes> getMyAuctions(Long userId, Pageable pageable) {
        Page<Auction> auctions = auctionRepository.findAllByItem_Seller_Id(userId, pageable);
        return auctions.map(AuctionRes::from);
    }

    /**
     * 관리자 통계용 - 날짜별 전체 경매 수
     */
    public long countByDate(LocalDate date) {
        return auctionRepository.findAll().stream()
                .filter(a -> a.getCreatedAt().toLocalDate().equals(date))
                .count();
    }

    /**
     * 관리자 통계용 - 날짜별 낙찰 성공 경매 수
     */
    public long countSuccessByDate(LocalDate date) {
        return auctionRepository.findAll().stream()
                .filter(a -> a.getStatus() == AuctionStatus.SUCCESS)
                .filter(a -> a.getEndAt() != null && a.getEndAt().toLocalDate().equals(date))
                .count();
    }

    /**
     * 관리자 통계용 - 날짜별 유찰 경매 수
     */
    public long countFailedByDate(LocalDate date) {
        return auctionRepository.findAll().stream()
                .filter(a -> a.getStatus() == AuctionStatus.FAILED)
                .filter(a -> a.getEndAt() != null && a.getEndAt().toLocalDate().equals(date))
                .count();
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
        //  PRICE 정렬 → Redis ZSET 기반 QueryService로 분기
        if (sort != null && sort.isRedisPriceSort()) {
            // categoryId 없으면 결과를 비우거나 예외 처리(선택)
            if (categoryId == null) {
                return new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
            }
            //category + price 정확이므로 status/keyword는 null만 허용하거나(엄격),
            if (status != null || (keyword != null && !keyword.isBlank())) {
                // category+price만 정확 지원
                return new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
            }

            return auctionQueryService.searchByCategoryPriceSorted(userId, categoryId, sort, page, size);
        }


        Pageable pageable = PageRequest.of(page, size, sort == null ? Sort.by(Sort.Direction.DESC,
                "startAt") : sort.toSort());

        var spec = AuctionSpecs.filter(status, categoryId, keyword);

        Page<Auction> auctions = auctionRepository.findAll(spec, pageable);

        // 찜 여부 미리 계산
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
