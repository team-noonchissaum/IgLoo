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
import org.springframework.data.domain.*;
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
    private final UserViewRedisLogger userViewRedisLogger; // ?ъ슜??議고쉶 Redis 濡쒓굅 二쇱엯
    private final noonchissaum.backend.recommendation.service.RecommendationService recommendationService; // 異붿쿇 ?쒕퉬??二쇱엯


    /**
     * ?덈줈??寃쎈ℓ瑜??깅줉?⑸땲??
     * ?곹뭹 ?뺣낫(Item)瑜?癒쇱? ?앹꽦?섍퀬, 愿???대?吏? 寃쎈ℓ ?쇱젙(Auction)???섎굹???몃옖??뀡?쇰줈 臾띠뼱 ??ν빀?덈떎.
     */
    @Transactional
    public Long registerAuction(Long userId, AuctionRegisterReq request) {
        User seller = userService.getUserByUserId(userId);
        Category category = categoryService.getCategory(request.getCategoryId());

        // 1. ?곹뭹(Item) ?뺣낫 ?앹꽦 + ?대?吏 ?깅줉
        Item item = itemService.createItem(seller,category,request);

        // 3. 寃쎈ℓ(Auction) ?ㅼ젙 諛????
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

        // price index 珥덇린媛??명똿
        Long categoryId = category.getId();
        auctionIndexService.updatePriceIndex(auction.getId(), categoryId, auction.getCurrentPrice());

        return auction.getId();
    }

    /**
     * 寃쎈ℓ 紐⑸줉??議고쉶?⑸땲?? N+1 臾몄젣瑜?諛⑹??섍린 ?꾪빐 Fetch Join???곸슜??Repository 硫붿꽌?쒕? ?ъ슜?⑸땲??
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
     * 寃쎈ℓ ?곸꽭 ?뺣낫瑜?議고쉶?⑸땲??
     */
    public AuctionRes getAuctionDetail(Long userId, Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_AUCTIONS));
        boolean isWished = wishService.isWished(userId, auction.getItem().getId());

        // ?ъ슜??議고쉶 湲곕줉 濡쒓퉭 (?몄쬆???ъ슜?먯씤 寃쎌슦?먮쭔 濡쒓퉭)
        if (userId != null) {
            userViewRedisLogger.logView(userId, auction.getItem().getId());
        }

        // 異붿쿇 寃쎈ℓ 媛?몄삤湲?
        List<AuctionRes> recommendedAuctions = recommendationService.getRecommendedAuctions(userId, auction.getItem().getId(), auction.getId());

        return AuctionRes.from(auction, isWished, recommendedAuctions);
    }

    /**
     * 寃쎈ℓ瑜?痍⑥냼?⑸땲??
     * ?먮ℓ??蹂몄씤 ?뺤씤怨??낆같??議댁옱 ?щ?瑜?寃利앺븯??寃쎈ℓ 臾닿껐?깆쓣 ?좎??⑸땲?ㅳ?
     */
    @Transactional
    public void cancelAuction(Long userId, Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_AUCTIONS));

        // ?먮ℓ??蹂몄씤 ?щ? ?뺤씤
        if (!auction.getItem().getSeller().getId().equals(userId)) {
            throw new CustomException(ErrorCode.AUCTION_NOT_OWNER);
        }

        // ?대? ?낆같??吏꾪뻾??寃쎌슦 痍⑥냼 遺덇? (鍮꾩쫰?덉뒪 洹쒖튃)
        if (auction.getBidCount() > 0) {
            throw new CustomException(ErrorCode.AUCTION_HAS_BIDS);
        }

        /**
         * 5遺꾩씠 吏?ъ쓣 寃쎌슦 蹂댁쬆湲??뚯닔?섎뒗 濡쒖쭅怨?5遺꾩씠?꾩씠硫?蹂댁쬆湲??뚮젮二쇰뒗 濡쒖쭅 異붽?
         * 5遺??댄썑 痍⑥냼??泥섎━?섎젮硫?RUNNING ?곹깭??痍⑥냼 ?덉슜
         */
        if (!(auction.getStatus() == AuctionStatus.READY || auction.getStatus() == AuctionStatus.RUNNING)) {
            throw new CustomException(ErrorCode.AUCTION_INVALID_STATUS);
        }

        // ?뺤콉 湲곗?: ?깅줉 + 5遺?
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime penaltyAt = auction.getCreatedAt().plusMinutes(5);

        int amount = (int) Math.min( auction.getCurrentPrice().longValue() * 0.05 , 1000);

        if (now.isBefore(penaltyAt)) {
            // 5遺??대궡 痍⑥냼 ??蹂댁쬆湲??섎텋 泥섎━
            auction.refundDeposit();

             walletService.setAuctionDeposit(userId, auctionId, amount, "refund");
        } else {
            // 5遺??댄썑 痍⑥냼 ??蹂댁쬆湲?紐곗닔 ?뺤젙(?⑤꼸??
            // 5遺??댄썑 痍⑥냼 -> 蹂댁쬆湲??섎텋 ?놁쓬
            auction.forfeitDeposit();
            walletService.setAuctionDeposit(userId, auctionId, amount, "forfeit");
        }


        //寃쎈ℓ 痍⑥냼???곹깭 蹂寃?
        auction.cancel();

        //radis?먯꽌 ??젣
        auctionRedisService.cancelAuction(auctionId);
    }

    /**
     * ?닿? ?깅줉??寃쎈ℓ 紐⑸줉 議고쉶
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
        //  PRICE ?뺣젹 ??Redis ZSET 湲곕컲 QueryService濡?遺꾧린
        if (sort != null && sort.isRedisPriceSort()) {
            // categoryId ?놁쑝硫?寃곌낵瑜?鍮꾩슦嫄곕굹 ?덉쇅 泥섎━(?좏깮)
            if (categoryId == null) {
                return new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
            }
            //category + price ?뺥솗?대?濡?status/keyword??null留??덉슜?섍굅???꾧꺽),
            if (status != null || (keyword != null && !keyword.isBlank())) {
                // category+price留??뺥솗 吏??
                return new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
            }

            return auctionQueryService.searchByCategoryPriceSorted(userId, categoryId, sort, page, size);
        }


        Pageable pageable = PageRequest.of(page, size, sort == null ? Sort.by(Sort.Direction.DESC,
                "startAt") : sort.toSort());

        var spec = AuctionSpecs.filter(status, categoryId, keyword);

        Page<Auction> auctions = auctionRepository.findAll(spec, pageable);

        // 李??щ? 誘몃━ 怨꾩궛
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
