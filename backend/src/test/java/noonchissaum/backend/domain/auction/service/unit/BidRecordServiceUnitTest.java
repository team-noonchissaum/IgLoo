package noonchissaum.backend.domain.auction.service.unit;

import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.Bid;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.auction.service.BidRecordService;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BidRecordServiceUnitTest {

    @Mock
    private BidRepository bidRepository;
    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private UserService userService;

    @Test
    @DisplayName("입찰 기록 저장 시 경매와 사용자 기반으로 Bid 엔티티 저장")
    void saveBidRecord_savesBidEntity() {
        BidRecordService service = new BidRecordService(bidRepository, auctionRepository, userService);
        Auction auction = sampleAuction(11L, sampleUser(1L, "seller"), "bid-record");
        User bidder = sampleUser(2L, "bidder");
        when(auctionRepository.findById(11L)).thenReturn(Optional.of(auction));
        when(userService.getUserByUserId(2L)).thenReturn(bidder);

        service.saveBidRecord(11L, 2L, BigDecimal.valueOf(12000), "req-11");

        ArgumentCaptor<Bid> captor = ArgumentCaptor.forClass(Bid.class);
        verify(bidRepository).save(captor.capture());
        Bid saved = captor.getValue();
        assertThat(saved.getAuction()).isEqualTo(auction);
        assertThat(saved.getBidder()).isEqualTo(bidder);
        assertThat(saved.getBidPrice()).isEqualByComparingTo("12000");
        assertThat(saved.getRequestId()).isEqualTo("req-11");
    }

    @Test
    @DisplayName("입찰 기록 저장 시 경매 미존재이면 NOT_FOUND_AUCTIONS 예외 던짐")
    void saveBidRecord_whenAuctionMissing_throwsApiException() {
        BidRecordService service = new BidRecordService(bidRepository, auctionRepository, userService);
        when(auctionRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.saveBidRecord(99L, 2L, BigDecimal.valueOf(12000), "req-99"));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_AUCTIONS);
    }

    private Auction sampleAuction(Long auctionId, User seller, String suffix) {
        Category category = new Category("category-" + suffix, null);
        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title("item-" + suffix)
                .description("desc-" + suffix)
                .startPrice(BigDecimal.valueOf(10000))
                .build();
        ReflectionTestUtils.setField(item, "id", auctionId + 1000);

        Auction auction = Auction.builder()
                .item(item)
                .startPrice(BigDecimal.valueOf(10000))
                .startAt(LocalDateTime.now().minusHours(1))
                .endAt(LocalDateTime.now().plusHours(1))
                .build();
        ReflectionTestUtils.setField(auction, "id", auctionId);
        return auction;
    }

    private User sampleUser(Long userId, String suffix) {
        User user = User.builder()
                .email("bid-record-unit-" + suffix + "@test.com")
                .nickname("bid_record_unit_" + suffix)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
