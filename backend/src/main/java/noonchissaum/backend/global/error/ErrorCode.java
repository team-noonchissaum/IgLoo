package noonchissaum.backend.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    LOW_BID_AMOUNT(HttpStatus.BAD_REQUEST, "B001", "현재가보다 같거나 낮은 가격에 입찰할 수 없습니다.");


    private final HttpStatus status;
    private final String message;
    private final String code;
}
