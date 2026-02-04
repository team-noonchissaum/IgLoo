package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 차단된 경매 게시글 복구 응답
 */

@Getter
@AllArgsConstructor
public class AdminAuctionRestoreRes {

    private Long auctionId;
    private String status;
    private LocalDateTime restoredAt;
}
