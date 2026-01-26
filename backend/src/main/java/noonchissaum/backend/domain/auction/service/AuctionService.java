package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.dto.AuctionRegisterReq;
import noonchissaum.backend.domain.auction.dto.AuctionRes;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.category.entity.Category;

import noonchissaum.backend.domain.category.service.CategoryService;
import noonchissaum.backend.domain.item.entity.Item;

import noonchissaum.backend.domain.item.service.ItemService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final ItemService itemService;
    private final UserService userService;
    private final CategoryService categoryService;


    /**
     * 새로운 경매를 등록합니다.
     * 상품 정보(Item)를 먼저 생성하고, 관련 이미지와 경매 일정(Auction)을 하나의 트랜잭션으로 묶어 저장합니다.
     */
    @Transactional
    public Long registerAuction(Long userId, AuctionRegisterReq request) {
        User seller = userService.getSeller(userId);
        Category category = categoryService.getcategory(request.getCategoryId());

        // 1. 상품(Item) 정보 생성 + 이미지 등록
        Item item = itemService.createItem(seller,category,request);

        // 3. 경매(Auction) 설정 및 저장
        Auction auction = Auction.builder()
                .item(item)
                .startPrice(request.getStartPrice())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .build();
        auctionRepository.save(auction);

        return auction.getId();
    }

    /**
     * 경매 목록을 조회합니다. N+1 문제를 방지하기 위해 Fetch Join이 적용된 Repository 메서드를 사용합니다.
     */
    public Page<AuctionRes> getAuctionList(Pageable pageable, AuctionStatus status) {
        Page<Auction> auctions;
        if (status != null) {
            auctions = auctionRepository.findAllByStatus(status, pageable);
        } else {
            auctions = auctionRepository.findAllWithItemAndSeller(pageable);
        }
        return auctions.map(AuctionRes::from);
    }

    /**
     * 경매 상세 정보를 조회합니다.
     */
    public AuctionRes getAuctionDetail(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));
        return AuctionRes.from(auction);
    }

    /**
     * 경매를 취소합니다.
     * 판매자 본인 확인과 입찰자 존재 여부를 검증하여 경매 무결성을 유지합니다。
     */
    @Transactional
    public void cancelAuction(Long userId, Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));

        // 판매자 본인 여부 확인
        if (!auction.getItem().getSeller().getId().equals(userId)) {
            throw new IllegalStateException("Only the seller can cancel the auction");
        }

        // 이미 입찰이 진행된 경우 취소 불가 (비즈니스 규칙)
        if (auction.getBidCount() > 0) {
            throw new IllegalStateException("Cannot cancel auction with existing bids");
        }

        /**
         * 5분이 지났을 경우 보증금 회수하는 로직과 5분이전이면 보증금 돌려주는 로칙 추가
         * 5분 이후 취소도 처리하려면 RUNNING 상태도 취소 허용
         */
        if (!(auction.getStatus() == AuctionStatus.READY || auction.getStatus() == AuctionStatus.RUNNING)) {
            throw new IllegalStateException("Cannot cancel auction in current status: " + auction.getStatus());
        }

        // 정책 기준: 등록 + 5분
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime penaltyAt = auction.getCreatedAt().plusMinutes(5);

        if (now.isBefore(penaltyAt)) {
            // 5분 이내 취소 → 보증금 환불 처리
            auction.refundDeposit();

            // TODO: 지갑팀 연동 시 여기서 DEPOSIT_REFUND 처리 호출
            // walletService.depositRefund(userId, auctionId, amount);
        } else {
            // 5분 이후 취소 → 보증금 몰수 확정(패널티)
            auction.forfeitDeposit();

            // TODO: 지갑팀 연동 시 여기서 DEPOSIT_FORFEIT 처리 호출
            // walletService.depositForfeit(userId, auctionId, amount);
        }


        //경매 취소시 상태 변경
        auction.cancel();
    }
}
