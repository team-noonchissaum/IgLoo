package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminAuctionBlockRes {

    private Long auctionId;
    private String status;
    private LocalDateTime blockedAt;
}
