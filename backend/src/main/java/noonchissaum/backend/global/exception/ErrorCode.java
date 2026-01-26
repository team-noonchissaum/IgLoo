package noonchissaum.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Bid Error
    LOW_BID_AMOUNT(HttpStatus.BAD_REQUEST, "B001", "현재가보다 같거나 낮은 가격에 입찰할 수 없습니다."),
    CANNOT_BID_CONTINUOUS(HttpStatus.TOO_MANY_REQUESTS, "B002", "연속적으로 입찰할 수 없습니다."),
    CANNOT_FIND_BID(HttpStatus.NOT_FOUND, "B003", "입찰을 찾을 수 없습니다."),
    DUPLICATE_BID_REQUEST(HttpStatus.CONFLICT, "B004", "이미 처리중인 입찰입니다."),
    BID_LOCK_ACQUISITION(HttpStatus.TOO_MANY_REQUESTS, "B005", "입찰자가 많아 처리에 실패했습니다. 다시 시도해주세요"),

    // Wallet Error
    INSUFFICIENT_BALANCE(HttpStatus.PAYMENT_REQUIRED, "W001", "잔액이 부족합니다."),
    CANNOT_FIND_WALLET(HttpStatus.NOT_FOUND, "W002", "지갑을 찾을 수 없습니다."),

    // Auction Error
    NOT_FOUND_AUCTIONS(HttpStatus.NOT_FOUND, "A001", "경매를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
