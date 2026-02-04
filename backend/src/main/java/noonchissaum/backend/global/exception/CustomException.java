package noonchissaum.backend.global.exception;

import lombok.Getter;

/**
 * 사용 예시:
 * throw new CustomException(ErrorCode.USER_NOT_FOUND);
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String customMessage;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.customMessage = null;
    }

    // 커스텀 메시지 생성자 추가
    public CustomException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.customMessage = customMessage;
    }

    @Override
    public String getMessage() {
        return customMessage != null ? customMessage : errorCode.getMessage();
    }

}
