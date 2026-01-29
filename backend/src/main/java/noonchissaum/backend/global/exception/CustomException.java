package noonchissaum.backend.global.exception;

import lombok.Getter;

/**
 * 사용 예시:
 * throw new CustomException(ErrorCode.USER_NOT_FOUND);
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
