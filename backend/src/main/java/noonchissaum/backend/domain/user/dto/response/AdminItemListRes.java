package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 차단된 게시글 목록 조회 응답
 */

@Getter
@AllArgsConstructor
public class AdminItemListRes {

    private Long itemId;
    private String title;
    private Long sellerId;
    private String sellerNickname;
    private Long startPrice;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime blockedAt;
    private String blockReason;

}
