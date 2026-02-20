package noonchissaum.backend.domain.auction.service.unit;

import noonchissaum.backend.domain.auction.dto.ws.BidSucceededPayload;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.entity.Bid;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.auction.service.AuctionIndexService;
import noonchissaum.backend.domain.auction.service.AuctionMessageService;
import noonchissaum.backend.domain.auction.service.AuctionRedisService;
import noonchissaum.backend.domain.auction.service.BidRollbackService;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.wallet.service.WalletRecordService;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.util.UserLockExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BidRollbackServiceUnitTest {

    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private BidRepository bidRepository;
    @Mock
    private AuctionRedisService auctionRedisService;
    @Mock
    private AuctionIndexService auctionIndexService;
    @Mock
    private AuctionMessageService auctionMessageService;
    @Mock
    private WalletService walletService;
    @Mock
    private WalletRecordService walletRecordService;
    @Mock
    private UserLockExecutor userLockExecutor;

    @Test
    @DisplayName("경쟁 입찰이 있으면 차단 유저 입찰 롤백 후 지갑/인덱스/메시지 갱신")
    void rollbackAuctionsForBlockedUser_withCompetingBids_rollsBack() {
        BidRollbackService service = new BidRollbackService(
                auctionRepository,
                bidRepository,
                auctionRedisService,
                auctionIndexService,
                auctionMessageService,
                walletService,
                walletRecordService,
                userLockExecutor
        );

        Auction auction = mock(Auction.class);
        Item item = mock(Item.class);
        Category category = mock(Category.class);
        User previousBidder = mock(User.class);
        User blockedBidder = mock(User.class);
        Bid bid1 = mock(Bid.class);
        Bid bid2 = mock(Bid.class);

        when(auction.getId()).thenReturn(10L);
        when(auction.getItem()).thenReturn(item);
        when(item.getCategory()).thenReturn(category);
        when(category.getId()).thenReturn(4L);
        when(auction.getCurrentPrice()).thenReturn(BigDecimal.valueOf(5000));
        when(auction.getEndAt()).thenReturn(LocalDateTime.now().plusMinutes(10));
        when(auction.getIsExtended()).thenReturn(false);

        when(previousBidder.getId()).thenReturn(20L);
        when(blockedBidder.getId()).thenReturn(99L);

        when(bid1.getBidder()).thenReturn(previousBidder);
        when(bid1.getBidPrice()).thenReturn(BigDecimal.valueOf(3000));
        when(bid2.getBidder()).thenReturn(blockedBidder);

        when(auctionRepository.findByCurrentBidder_IdAndStatusIn(99L, List.of(AuctionStatus.RUNNING, AuctionStatus.DEADLINE)))
                .thenReturn(List.of(auction));
        when(bidRepository.findByAuctionIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(bid1, bid2));
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return null;
        }).when(userLockExecutor).withUserLocks(eq(List.of(20L, 99L)), any(Runnable.class));

        service.rollbackAuctionsForBlockedUser(99L);

        verify(bidRepository).deleteByAuctionIdAndBidderId(10L, 99L);
        verify(auction).rollbackBid(previousBidder, BigDecimal.valueOf(3000), 1);
        verify(auctionRepository).save(auction);
        verify(walletRecordService).rollbackWalletRecord(99L, 20L, BigDecimal.valueOf(5000), BigDecimal.valueOf(3000), 10L);
        verify(walletService).rollbackBidWallet(99L, 20L, BigDecimal.valueOf(5000), BigDecimal.valueOf(3000));
        verify(walletService).clearWalletCache(99L);
        verify(walletService).clearWalletCache(20L);
        verify(auctionIndexService).updatePriceIndex(10L, 4L, BigDecimal.valueOf(3000));

        ArgumentCaptor<BidSucceededPayload> payloadCaptor = ArgumentCaptor.forClass(BidSucceededPayload.class);
        verify(auctionMessageService).sendBidSucceeded(eq(10L), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().getCurrentPrice()).isEqualTo(3000L);
        assertThat(payloadCaptor.getValue().getCurrentBidderId()).isEqualTo(20L);
        assertThat(payloadCaptor.getValue().getBidCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("경쟁 입찰이 없으면 롤백을 수행하지 않음")
    void rollbackAuctionsForBlockedUser_withoutCompetingBids_skipsRollback() {
        BidRollbackService service = new BidRollbackService(
                auctionRepository,
                bidRepository,
                auctionRedisService,
                auctionIndexService,
                auctionMessageService,
                walletService,
                walletRecordService,
                userLockExecutor
        );

        Auction auction = mock(Auction.class);
        User blockedBidder = mock(User.class);
        Bid onlyBlockedBid = mock(Bid.class);

        when(auction.getId()).thenReturn(99L);
        when(blockedBidder.getId()).thenReturn(7L);
        when(onlyBlockedBid.getBidder()).thenReturn(blockedBidder);

        when(auctionRepository.findByCurrentBidder_IdAndStatusIn(7L, List.of(AuctionStatus.RUNNING, AuctionStatus.DEADLINE)))
                .thenReturn(List.of(auction));
        when(bidRepository.findByAuctionIdOrderByCreatedAtAsc(99L)).thenReturn(List.of(onlyBlockedBid));

        service.rollbackAuctionsForBlockedUser(7L);

        verify(bidRepository, never()).deleteByAuctionIdAndBidderId(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
        verify(walletRecordService, never()).rollbackWalletRecord(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong());
    }
}
