package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 차단된 경매 게시글 목록 조회 응답
 */

@Getter
@AllArgsConstructor
public class AdminBlockedAuctionRes {

    private Long auctionId;
    private String title;
    private Long sellerId;
    private String sellerNickname;
    private String reason;
    private LocalDateTime blockedAt;
}
