package noonchissaum.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ========== COMMON (공통 에러) ==========
    /** 서버 내부 오류 */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", "COMMON-001"),
    /** 요청 값 검증 실패 */
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다.", "COMMON-002"),

    // ========== AUTH (인증/인가 에러) ==========
    /** 로그인 실패 - 이메일 또는 비밀번호 불일치 */
    INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.", "AUTH-001"),
    /** 접근 권한 없음 (ADMIN 전용 API 등) */
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", "AUTH-002"),
    /** 인증 필요 - 토큰 없이 접근 */
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.", "AUTH-003"),
    /** 토큰 만료 */
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.", "AUTH-004"),
    /** 유효하지 않은 토큰 */
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", "AUTH-005"),
    /** 리프레시 토큰 오류 */
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 올바르지 않습니다.", "AUTH-006"),
    /** 차단된 사용자 - 로그인 불가능 */
    USER_BLOCKED(HttpStatus.FORBIDDEN, "차단된 사용자입니다. 관리자에게 문의하세요.", "AUTH-007"),

    // ========== USER (사용자 에러) ==========
    /** 이메일 중복 */
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.", "USER-001"),
    /** 닉네임 중복 */
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.", "USER-002"),
    /** 사용자 조회 실패 */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다.", "USER-003"),
    /** 이미 차단된 사용자 재차단 시도 */
    USER_ALREADY_BLOCKED(HttpStatus.BAD_REQUEST, "이미 차단된 사용자입니다.", "USER-004"),
    /** 차단되지 않은 사용자 해제 시도 */
    USER_NOT_BLOCKED(HttpStatus.BAD_REQUEST, "차단되지 않은 사용자입니다.", "USER-005"),

    // ========== REPORT (신고 에러) ==========
    /** 신고 조회 실패 */
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다.", "REPORT-001"),
    REPORT_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "이미 처리된 신고입니다.", "REPORT-002"),

    // ========== ITEM (상품/게시글 에러) ==========
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.", "ITEM-001"),

  
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
    NOT_FOUND_AUCTIONS(HttpStatus.NOT_FOUND, "A001", "경매를 찾을 수 없습니다."),

    // Payment Error
    INVALID_PAYMENT_REQUEST(HttpStatus.BAD_REQUEST, "P001", "충전 금액이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
