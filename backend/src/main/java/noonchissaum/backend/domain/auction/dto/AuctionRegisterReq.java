package noonchissaum.backend.domain.auction.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class AuctionRegisterReq {
    private String title;
    private String description;
    private BigDecimal startPrice;
    private Long categoryId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private List<String> imageUrls; // Base64 or URLs. Here assumes URLs or handles separately.
}