package noonchissaum.backend.domain.auction.service.unit;

import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.service.AuctionIndexService;
import noonchissaum.backend.domain.auction.service.AuctionMessageService;
import noonchissaum.backend.domain.auction.service.AuctionRecordService;
import noonchissaum.backend.domain.auction.service.AuctionRedisService;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuctionRecordServiceUnitTest {

    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private UserService userService;
    @Mock
    private AuctionRedisService auctionRedisService;
    @Mock
    private AuctionIndexService auctionIndexService;
    @Mock
    private AuctionMessageService auctionMessageService;

    @Test
    @DisplayName("입찰 저장 시 Auction/Redis/가격 인덱스를 갱신")
    void saveAuction_updatesBidAndIndex() {
        AuctionRecordService service = new AuctionRecordService(
                auctionRepository,
                userService,
                auctionRedisService,
                auctionIndexService,
                auctionMessageService
        );
        Auction auction = sampleAuction(10L, 999L);
        User bidder = sampleUser(3L, "bidder");

        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(userService.getUserByUserId(3L)).thenReturn(bidder);

        service.saveAuction(10L, 3L, BigDecimal.valueOf(25000));

        assertThat(auction.getCurrentBidder()).isEqualTo(bidder);
        assertThat(auction.getCurrentPrice()).isEqualByComparingTo(BigDecimal.valueOf(25000));
        assertThat(auction.getBidCount()).isEqualTo(1);
        verify(auctionRedisService).setRedis(10L);
        verify(auctionIndexService).updatePriceIndex(10L, 999L, BigDecimal.valueOf(25000));
    }

    @Test
    @DisplayName("연장 가능한 시점 업데이트면 연장 메시지를 전송")
    void updateAuctionWithExtension_whenExtended_sendsExtendedMessage() {
        AuctionRecordService service = new AuctionRecordService(
                auctionRepository,
                userService,
                auctionRedisService,
                auctionIndexService,
                auctionMessageService
        );
        Auction auction = sampleAuction(20L, 555L);
        ReflectionTestUtils.setField(auction, "endAt", LocalDateTime.now().plusMinutes(1));
        User bidder = sampleUser(4L, "bidder2");

        when(auctionRepository.findById(20L)).thenReturn(Optional.of(auction));
        when(userService.getUserByUserId(4L)).thenReturn(bidder);

        service.updateAuctionWithExtension(20L, 4L, BigDecimal.valueOf(30000));

        verify(auctionRedisService).setRedis(20L);
        verify(auctionMessageService).sendAuctionExtended(org.mockito.ArgumentMatchers.eq(20L), org.mockito.ArgumentMatchers.any());
        verify(auctionIndexService).updatePriceIndex(20L, 555L, BigDecimal.valueOf(30000));
    }

    @Test
    @DisplayName("경매 미존재 시 NOT_FOUND_AUCTIONS 예외")
    void saveAuction_whenAuctionMissing_throwsApiException() {
        AuctionRecordService service = new AuctionRecordService(
                auctionRepository,
                userService,
                auctionRedisService,
                auctionIndexService,
                auctionMessageService
        );
        when(auctionRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.saveAuction(99L, 1L, BigDecimal.TEN));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_AUCTIONS);
        verify(userService, never()).getUserByUserId(org.mockito.ArgumentMatchers.anyLong());
    }

    private Auction sampleAuction(Long auctionId, Long categoryId) {
        User seller = sampleUser(11L, "seller");
        Category category = new Category("cat", null);
        ReflectionTestUtils.setField(category, "id", categoryId);
        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title("item")
                .description("desc")
                .startPrice(BigDecimal.valueOf(10000))
                .build();

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
                .email("auction-record-" + suffix + "@test.com")
                .nickname("auction_record_" + suffix)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
