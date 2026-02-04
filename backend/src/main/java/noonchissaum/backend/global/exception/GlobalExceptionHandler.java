package noonchissaum.backend.global.exception;

import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.global.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomException 처리
     * - 비즈니스 로직에서 발생한 의도된 예외
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Map<String, Object>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("CustomException 발생: {}", errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(Map.of(
                        "success", false,
                        "code", errorCode.getCode(),
                        "message", errorCode.getMessage()
                ));
    }

    /**
     * @Valid 검증 실패 처리
     * - DTO의 @NotBlank, @Email 등 검증 실패 시
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError.getField() + " : " + fieldError.getDefaultMessage();

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(Map.of(
                        "success", false,
                        "code", ErrorCode.INVALID_INPUT_VALUE.getCode(),
                        "message", message
                ));
    }

    /**
     * JSON 파싱 실패 처리
     * - 요청 본문이 올바른 JSON 형식이 아닐 때
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonParse(HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException", e);

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(Map.of(
                        "success", false,
                        "code", ErrorCode.INVALID_INPUT_VALUE.getCode(),
                        "message", "요청 형식이 올바르지 않습니다."
                ));
    }

    /**
     * IllegalArgumentException 처리
     * - 잘못된 인자 전달 시
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(Map.of(
                        "success", false,
                        "code", ErrorCode.INVALID_INPUT_VALUE.getCode(),
                        "message", e.getMessage()
                ));
    }

    /**
     * 이미지 파일 크기 초과 예외 처리
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e
    ) {
        log.warn("파일 크기 초과: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("파일 크기가 너무 큽니다. 최대 10MB까지 업로드 가능합니다."));
    }

    /**
     * 그 외 모든 예외 처리
     * - 예상치 못한 서버 오류
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("Unhandled Exception 발생", e);

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(Map.of(
                        "success", false,
                        "code", ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        "message", ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
                ));
    }
}
