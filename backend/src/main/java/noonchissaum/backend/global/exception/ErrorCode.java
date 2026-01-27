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

    // ========== ITEM (상품/게시글 에러) ==========
    /** 게시글 조회 실패 */
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.", "ITEM-001"),
    /** 이미 차단된 게시글 재차단 시도 */
    ITEM_ALREADY_BLOCKED(HttpStatus.BAD_REQUEST, "이미 차단된 게시글입니다.", "ITEM-002"),
    /** 차단되지 않은 게시글 복구 시도 */
    ITEM_NOT_BLOCKED(HttpStatus.BAD_REQUEST, "차단되지 않은 게시글입니다.", "ITEM-003"),

    // ========== AUCTION (경매 에러) ==========
    /** 경매 조회 실패 */
    AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다.", "AUCTION-001"),

    // ========== WALLET (지갑/크레딧 에러) ==========
    /** 지갑 조회 실패 */
    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "지갑을 찾을 수 없습니다.", "WALLET-001"),
    /** 잔액 부족 */
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "잔액이 부족합니다.", "WALLET-002"),
    ;



    private final HttpStatus status;
    private final String message;
    private final String code;
}
