package noonchissaum.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ========== COMMON (공통 에러) ==========
    /** 서버 내부 오류 */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-001", "서버 내부 오류가 발생했습니다."),
    /** 요청 값 검증 실패 */
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다.", "COMMON-002"),
    /** 잘못된 요청 */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON-003", "잘못된 요청입니다."),
    /** 리소스를 찾을 수 없음 */
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON-004", "요청한 리소스를 찾을 수 없습니다."),
    // ========== AUTH (인증/인가 에러) ==========
    /** 로그인 실패 - 이메일 또는 비밀번호 불일치 */
    INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "AUTH-001", "이메일 또는 비밀번호가 올바르지 않습니다."),
    /** 접근 권한 없음 (ADMIN 전용 API 등) */
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH-002", "접근 권한이 없습니다."),
    /** 인증 필요 - 토큰 없이 접근 */
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH-003", "인증이 필요합니다."),
    /** 토큰 만료 */
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH-004", "토큰이 만료되었습니다."),
    /** 유효하지 않은 토큰 */
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH-005", "유효하지 않은 토큰입니다."),
    /** 리프레시 토큰 오류 */
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-006", "리프레시 토큰이 올바르지 않습니다."),
    /** 차단된 사용자 - 로그인 불가능 */
    USER_BLOCKED(HttpStatus.FORBIDDEN, "AUTH-007", "차단된 사용자입니다. 관리자에게 문의하세요."),

    OAUTH2_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH-008", "소셜 로그인에 실패했습니다."),

    // ========== USER (사용자 에러) ==========
    /** 이메일 중복 */
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER-001", "이미 사용 중인 이메일입니다."),
    /** 닉네임 중복 */
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USER-002", "이미 사용 중인 닉네임입니다."),
    /** 사용자 조회 실패 */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-003", "존재하지 않는 사용자입니다."),

    BALANCE_EXISTS(HttpStatus.CONFLICT,"USER-004","잔액이 남아있습니다."),

    // ========== BLOCK (차단 에러) ==========
    /** 이미 차단된 아이템 */
    ITEM_ALREADY_BLOCKED(HttpStatus.BAD_REQUEST, "BLOCK-001", "이미 차단된 게시글입니다."),
    /** 차단되지 않은 아이템 복구 시도 */
    ITEM_NOT_BLOCKED(HttpStatus.BAD_REQUEST, "BLOCK-002", "차단된 게시글이 아닙니다."),
    /** 차단되지 않은 경매 복구 시도 */
    AUCTION_NOT_BLOCKED(HttpStatus.BAD_REQUEST, "BLOCK-003", "차단된 경매가 아닙니다."),
    /** 이미 차단된 경매 */
    AUCTION_ALREADY_BLOCKED(HttpStatus.BAD_REQUEST, "BLOCK-004", "이미 차단된 경매입니다."),
    /** 이미 차단된 사용자 재차단 시도 */
    USER_ALREADY_BLOCKED(HttpStatus.BAD_REQUEST, "BLOCK-005", "이미 차단된 사용자입니다."),
    /** 차단되지 않은 사용자 해제 시도 */
    USER_NOT_BLOCKED(HttpStatus.BAD_REQUEST, "BLOCK-006", "차단되지 않은 사용자입니다."),
    /** 진행중인 게시물이 아닌 것을 차단 시도 */
    AUCTION_CANNOT_BLOCK(HttpStatus.BAD_REQUEST, "BLOCK-007", "진행 중인 경매만 차단할 수 있습니다."),

    // ===== FILE 관련 =====
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F001", "파일 업로드에 실패했습니다."),

    // ========== REPORT (신고 에러) ==========
    /** 신고 조회 실패 */
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT-001", "신고를 찾을 수 없습니다."),
    /**이미 처리된 신고*/
    REPORT_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "REPORT-002", "이미 처리된 신고입니다."),
    /**이미 신고했던 기록이 있는 경매*/
    ALREADY_REPORTED(HttpStatus.ALREADY_REPORTED, "REPORT-003", "이미 신고된 경매입니다"),
    /**옳지 않은 신고 대상*/
    INVALID_REPORT_TARGET(HttpStatus.NOT_FOUND, "REPORT-004", "대상 유저를 찾을 수 없습니다"),

    // ========== ITEM (상품/게시글 에러) ==========
    /**게시글 없음*/
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "ITEM-001", "게시글을 찾을 수 없습니다."),
    /**이미 삭제된 게시글 재 삭제 시도*/
    ITEM_ALREADY_DELETED(HttpStatus.NOT_FOUND, "ITEM-002", "이미 삭제된 게시글입니다"),

    // ========== CATEGORY (카테고리 에러) ==========
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY-001", "카테고리를 찾을 수 없습니다."),

    // Bid Error
    LOW_BID_AMOUNT(HttpStatus.BAD_REQUEST, "B001", "현재가보다 같거나 낮은 가격에 입찰할 수 없습니다."),
    CANNOT_BID_CONTINUOUS(HttpStatus.TOO_MANY_REQUESTS, "B002", "연속적으로 입찰할 수 없습니다."),
    CANNOT_FIND_BID(HttpStatus.NOT_FOUND, "B003", "입찰을 찾을 수 없습니다."),
    DUPLICATE_BID_REQUEST(HttpStatus.CONFLICT, "B004", "이미 처리중인 입찰입니다."),
    BID_LOCK_ACQUISITION(HttpStatus.TOO_MANY_REQUESTS, "B005", "입찰자가 많아 처리에 실패했습니다. 다시 시도해주세요"),

    // Wallet Error
    INSUFFICIENT_BALANCE(HttpStatus.PAYMENT_REQUIRED, "W001", "잔액이 부족합니다."),
    CANNOT_FIND_WALLET(HttpStatus.NOT_FOUND, "W002", "지갑을 찾을 수 없습니다."),
    INSUFFICIENT_LOCKED_BALANCE(HttpStatus.BAD_REQUEST,"W003","잠긴 잔액이 부족합니다."),

    // Auction Error
    NOT_FOUND_AUCTIONS(HttpStatus.NOT_FOUND, "A001", "경매를 찾을 수 없습니다."),
    AUCTION_NOT_OWNER(HttpStatus.FORBIDDEN, "A002", "해당 경매의 판매자가 아닙니다."),
    AUCTION_HAS_BIDS(HttpStatus.BAD_REQUEST, "A003", "입찰이 존재하는 경매는 취소할 수 없습니다."),
    AUCTION_INVALID_STATUS(HttpStatus.BAD_REQUEST, "A004", "현재 상태에서는 경매를 취소할 수 없습니다."),
    AUCTION_BLOCKED(HttpStatus.BAD_REQUEST, "A005", "차단된 경매입니다."),
    AUCTION_RESTORE_EXPIRED(HttpStatus.BAD_REQUEST, "A006", "종료된 경매는 복구할 수 없습니다."),
    AUCTION_STATUS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "A005", "경매 상태 정보를 불러올 수 없습니다."),

    // Notification Error
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "알림을 찾을 수 없습니다."),
    //Charge Error
    CHARGE_LOCK_ACQUISITION(HttpStatus.TOO_MANY_REQUESTS, "C001", "처리에 실패했습니다. 다시 시도해주세요"),
    CHARGE_CHECK_NOT_FOUND(HttpStatus.NOT_FOUND,"C002","충전금을 찾을 수 없습니다."),
    CHARGE_CANCELED(HttpStatus.CONFLICT,"C003","이미 취소 처리된 충전금입니다."),
    CHARGE_CONFIRMED(HttpStatus.CONFLICT,"C004","이미 충전 처리된 충전금입니다"),
    NOT_FOUND_CHARGE(HttpStatus.NOT_FOUND,"C005","이미 충전 처리된 충전금입니다"),
    REFUND_DATE_EXPIRED(HttpStatus.BAD_REQUEST,"C006","환불 가능 기간이 만료되었습니다."),

    //Task Error
    PENDING_TASK_EXISTS(HttpStatus.CONFLICT,"T001","처리에 실패했습니다. 다시 시도해주세요"),

    // Payment Error
    INVALID_PAYMENT_REQUEST(HttpStatus.BAD_REQUEST, "P001", "충전 금액이 올바르지 않습니다."),
    NOT_FOUND_PAYMENT(HttpStatus.NOT_FOUND, "P002", "결제를 찾을 수 없습니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "P003", "결제 상태가 올바르지 않습니다."),
    ALREADY_EXISTS_PAYMENT(HttpStatus.CONFLICT, "P004", "이미 처리된 결제입니다."),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "P005", "결제 금액이 일치하지 않습니다."),
    PAYMENTS_FAILED(HttpStatus.BAD_GATEWAY, "P006", "결제 승인에 실패했습니다."),
    ALREADY_CANCELED_PAYMENT(HttpStatus.CONFLICT, "P007", "이미 취소된 결제입니다."),
    REFUND_FAILED(HttpStatus.BAD_GATEWAY, "P008", "환불 처리에 실패했습니다."),

    // Withdrawal Error
    WITHDRAW_MIN_AMOUNT(HttpStatus.BAD_REQUEST, "WD001", "출금 금액은 최소 10,000원 이상이어야 합니다."),
    WITHDRAW_NOT_FOUND(HttpStatus.NOT_FOUND, "WD002", "출금 신청 내역을 찾을 수 없습니다."),
    WITHDRAW_NOT_REQUESTED(HttpStatus.BAD_REQUEST, "WD003", "출금 승인 대기 상태가 아닙니다."),
    WITHDRAW_NOT_CANCELABLE(HttpStatus.BAD_REQUEST, "WD004", "취소할 수 없는 출금 상태입니다."),

    // Chat Error
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-001", "존재하지 않는 채팅방입니다."),

    // Chatbot Error
    CHAT_SCENARIO_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-001", "Chat scenario not found."),
    CHAT_NODE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-002", "Chat node not found."),
    CHAT_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-003", "Chat option not found."),

    //Lock
    LOCK_ACQUISITION(HttpStatus.TOO_MANY_REQUESTS, "L001", "락 획득에 실패했습니다. 다시 시도해주세요"),

    // Coupons Error
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "CPN-001", "쿠폰을 찾을 수 없습니다."),
    NO_AUTHORIZED_COUPON_USE(HttpStatus.FORBIDDEN, "CPN-002", "쿠폰을 사용할 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
