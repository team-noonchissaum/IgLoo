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
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON-002", "요청 값이 올바르지 않습니다."),
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
    PASSWORD_RESET_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH-009", "비밀번호 재설정 토큰이 유효하지 않습니다."),
    PASSWORD_RESET_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH-010", "비밀번호 재설정 토큰이 만료되었습니다."),
    PASSWORD_RESET_LOCAL_ONLY(HttpStatus.BAD_REQUEST, "AUTH-011", "로컬 계정만 비밀번호를 재설정할 수 있습니다."),
    PASSWORD_RESET_MAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH-012", "비밀번호 재설정 메일 발송에 실패했습니다."),

    // ========== USER (사용자 에러) ==========
    /** 이메일 중복 */
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER-001", "이미 사용 중인 이메일입니다."),
    /** 닉네임 중복 */
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USER-002", "이미 사용 중인 닉네임입니다."),
    /** 사용자 조회 실패 */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-003", "존재하지 않는 사용자입니다."),

    BALANCE_EXISTS(HttpStatus.CONFLICT,"USER-004","잔액이 남아있습니다."),
    USER_DELETED(HttpStatus.FORBIDDEN, "USER-005", "탈퇴한 사용자입니다."),
    USER_ALREADY_DELETED(HttpStatus.CONFLICT, "USER-006", "이미 탈퇴한 사용자입니다."),

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
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F002", "파일 삭제에 실패했습니다."),
    FILE_URL_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F003", "파일 URL 생성에 실패했습니다."),

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
    CATEGORY_DELETE_FORBIDDEN(HttpStatus.BAD_REQUEST, "CATEGORY-002", "해당 카테고리는 삭제할 수 없습니다."),
    CATEGORY_ETC_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY-003", "기타 카테고리를 찾을 수 없습니다."),

    // Bid Error
    LOW_BID_AMOUNT(HttpStatus.BAD_REQUEST, "B001", "현재가보다 같거나 낮은 가격에 입찰할 수 없습니다."),
    CANNOT_BID_CONTINUOUS(HttpStatus.TOO_MANY_REQUESTS, "B002", "연속적으로 입찰할 수 없습니다."),
    CANNOT_FIND_BID(HttpStatus.NOT_FOUND, "B003", "입찰을 찾을 수 없습니다."),
    DUPLICATE_BID_REQUEST(HttpStatus.CONFLICT, "B004", "이미 처리중인 입찰입니다."),
    BID_LOCK_ACQUISITION(HttpStatus.TOO_MANY_REQUESTS, "B005", "입찰자가 많아 처리에 실패했습니다. 다시 시도해주세요"),
    BID_INVALID_PAGE(HttpStatus.BAD_REQUEST, "B006", "페이지 요청 값이 올바르지 않습니다."),

    // Wallet Error
    INSUFFICIENT_BALANCE(HttpStatus.PAYMENT_REQUIRED, "W001", "잔액이 부족합니다."),
    CANNOT_FIND_WALLET(HttpStatus.NOT_FOUND, "W002", "지갑을 찾을 수 없습니다."),
    INSUFFICIENT_LOCKED_BALANCE(HttpStatus.BAD_REQUEST,"W003","잠긴 잔액이 부족합니다."),
    WALLET_ACCESS_DENIED(HttpStatus.FORBIDDEN, "W004", "지갑에 접근할 수 없습니다."),
    WALLET_INVALID_CASE(HttpStatus.BAD_REQUEST, "W005", "지갑 처리 유형이 올바르지 않습니다."),

    // Auction Error
    NOT_FOUND_AUCTIONS(HttpStatus.NOT_FOUND, "A001", "경매를 찾을 수 없습니다."),
    AUCTION_NOT_OWNER(HttpStatus.FORBIDDEN, "A002", "해당 경매의 판매자가 아닙니다."),
    AUCTION_HAS_BIDS(HttpStatus.BAD_REQUEST, "A003", "입찰이 존재하는 경매는 취소할 수 없습니다."),
    AUCTION_INVALID_STATUS(HttpStatus.BAD_REQUEST, "A004", "현재 상태에서는 경매를 취소할 수 없습니다."),
    AUCTION_BLOCKED(HttpStatus.BAD_REQUEST, "A005", "차단된 경매입니다."),
    AUCTION_RESTORE_EXPIRED(HttpStatus.BAD_REQUEST, "A006", "종료된 경매는 복구할 수 없습니다."),
    AUCTION_STATUS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "A007", "경매 상태 정보를 불러올 수 없습니다."),
    AUCTION_NOT_RUNNING(HttpStatus.BAD_REQUEST, "A008", "현재 진행 중인 경매가 아닙니다."),
    AUCTION_ENDED(HttpStatus.BAD_REQUEST, "A009", "종료된 경매입니다."),
    AUCTION_NOT_HOTDEAL(HttpStatus.BAD_REQUEST, "A010", "핫딜 경매가 아닙니다."),
    AUCTION_REDIS_STATE_MISSING(HttpStatus.SERVICE_UNAVAILABLE, "A011", "경매 상태 정보가 없습니다."),
    AUCTION_REDIS_STATE_INVALID(HttpStatus.SERVICE_UNAVAILABLE, "A012", "경매 상태 정보가 올바르지 않습니다."),

    // Notification Error
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "알림을 찾을 수 없습니다."),
    NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "N002", "알림에 접근할 수 없습니다."),
    //Charge Error
    CHARGE_LOCK_ACQUISITION(HttpStatus.TOO_MANY_REQUESTS, "C001", "처리에 실패했습니다. 다시 시도해주세요"),
    CHARGE_CHECK_NOT_FOUND(HttpStatus.NOT_FOUND,"C002","충전금을 찾을 수 없습니다."),
    CHARGE_CANCELED(HttpStatus.CONFLICT,"C003","이미 취소 처리된 충전금입니다."),
    CHARGE_CONFIRMED(HttpStatus.CONFLICT,"C004","이미 충전 처리된 충전금입니다"),
    NOT_FOUND_CHARGE(HttpStatus.NOT_FOUND,"C005","충전 내역을 찾을 수 없습니다."),
    REFUND_DATE_EXPIRED(HttpStatus.BAD_REQUEST,"C006","환불 가능 기간이 만료되었습니다."),
    CHARGE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "C007", "충전 내역에 접근할 수 없습니다."),

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
    PAYMENT_STATUS_INVALID_FOR_APPROVE(HttpStatus.BAD_REQUEST, "P009", "현재 상태에서는 결제 승인할 수 없습니다."),
    PAYMENT_STATUS_INVALID_FOR_CANCEL(HttpStatus.BAD_REQUEST, "P010", "현재 상태에서는 결제를 취소할 수 없습니다."),
    REFUND_ACCESS_DENIED(HttpStatus.FORBIDDEN, "P011", "환불 권한이 없습니다."),
    WEBHOOK_INVALID_STATE(HttpStatus.BAD_REQUEST, "P012", "웹훅 상태가 올바르지 않습니다."),
    WEBHOOK_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "P013", "웹훅 금액이 일치하지 않습니다."),

    // Withdrawal Error
    WITHDRAW_MIN_AMOUNT(HttpStatus.BAD_REQUEST, "WD001", "출금 금액은 최소 10,000원 이상이어야 합니다."),
    WITHDRAW_NOT_FOUND(HttpStatus.NOT_FOUND, "WD002", "출금 신청 내역을 찾을 수 없습니다."),
    WITHDRAW_NOT_REQUESTED(HttpStatus.BAD_REQUEST, "WD003", "출금 승인 대기 상태가 아닙니다."),
    WITHDRAW_NOT_CANCELABLE(HttpStatus.BAD_REQUEST, "WD004", "취소할 수 없는 출금 상태입니다."),
    WITHDRAW_ALREADY_PROCESSED(HttpStatus.CONFLICT, "WD005", "이미 처리된 출금 요청입니다."),

    // Chat Error
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-001", "존재하지 않는 채팅방입니다."),
    CHAT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CHAT-005", "채팅방에 접근할 수 없습니다."),
    CHAT_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "CHAT-006", "채팅 요청 값이 올바르지 않습니다."),

    // Chatbot Error
    CHAT_SCENARIO_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-004", "Chat scenario not found."),
    CHAT_NODE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-002", "Chat node not found."),
    CHAT_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-003", "Chat option not found."),
    CHAT_SCENARIO_INACTIVE(HttpStatus.BAD_REQUEST, "CHAT-007", "Chat scenario is inactive."),

    // OAuth Error
    OAUTH_UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "OAUTH-001", "지원하지 않는 OAuth2 Provider 입니다."),
    OAUTH_PROVIDER_ID_INVALID(HttpStatus.BAD_REQUEST, "OAUTH-002", "OAuth2 providerId가 올바르지 않습니다."),

    // WebSocket Error
    WS_AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "WS-001", "WebSocket 인증이 필요합니다."),
    WS_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "WS-002", "WebSocket 토큰이 유효하지 않습니다."),
    WS_UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "WS-003", "WebSocket 세션이 인증되지 않았습니다."),
    WS_SUBSCRIBE_DENIED(HttpStatus.FORBIDDEN, "WS-004", "채팅 구독 권한이 없습니다."),
    WS_ADMIN_SEND_FORBIDDEN(HttpStatus.FORBIDDEN, "WS-005", "관리자는 채팅 전송이 불가합니다."),

    //Lock
    LOCK_ACQUISITION(HttpStatus.TOO_MANY_REQUESTS, "L001", "락 획득에 실패했습니다. 다시 시도해주세요"),

    // Coupons Error
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "CPN-001", "쿠폰을 찾을 수 없습니다."),
    NO_AUTHORIZED_COUPON_USE(HttpStatus.FORBIDDEN, "CPN-002", "쿠폰을 사용할 권한이 없습니다."),


    // Settlement Error
    INVALID_ORDER_STATUS_FOR_SETTLEMENT(HttpStatus.BAD_REQUEST, "ORDER-002", "정산을 진행할 수 없는 주문 상태입니다."),
    SETTLEMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "SETTLEMENT-001", "이미 정산이 완료된 주문입니다."),
    SETTLEMENT_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "SETTLEMENT-002", "정산 대상 사용자를 찾을 수 없습니다."),
    SETTLEMENT_WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "SETTLEMENT-003", "정산 대상 지갑을 찾을 수 없습니다."),

    // Order Error
    DELIVERY_TYPE_NOT_DIRECT(HttpStatus.BAD_REQUEST, "DELIVERY-002", "배송 타입이 직거래가 아닙니다."),
    INVALID_ORDER_AMOUNT(HttpStatus.BAD_REQUEST, "ORDER-003", "주문 금액이 올바르지 않습니다."),
    SHIPMENT_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "SHIP-003", "유효한 배송 요청 건을 찾을 수 없습니다."),
    SHIPMENT_ALREADY_SHIPPED(HttpStatus.BAD_REQUEST, "SHIP-004", "이미 발송 처리가 완료된 배송 건입니다."),
    SHIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "SHIP-001", "배송 정보를 찾을 수 없습니다."),
    DELIVERY_TYPE_NOT_SHIPMENT(HttpStatus.BAD_REQUEST, "DELIVERY-001", "배송 타입이 택배가 아닙니다.") ,
    SHIPMENT_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "SHIP-002", "이미 해당 주문에 대한 배송 정보가 존재합니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER-001", "존재하지 않는 주문입니다."),
    SHIPMENT_TRACKING_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "SHIP-005", "송장 정보가 없어 배송 조회가 불가능합니다."),
    SWEETTRACKER_API_ERROR(HttpStatus.BAD_GATEWAY, "SHIP-006", "택배 조회 서비스 응답이 올바르지 않습니다."),       // 외부 API 실패/응답 이상
    SHIPMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "SHIP-007", "배송 정보에 접근할 수 없습니다."),
    SHIPMENT_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "SHIP-008", "배송 요청 값이 올바르지 않습니다."),
    SWEETTRACKER_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "SHIP-009", "택배 조회 서비스 응답이 비정상입니다."),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ORDER-004", "주문에 대한 접근 권한이 없습니다."),
    ORDER_SHIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER-005", "주문에 대한 배송 정보를 찾을 수 없습니다."),
    ORDER_NOT_DELIVERED_YET(HttpStatus.BAD_REQUEST, "ORDER-006", "배송 완료 후에만 구매확정이 가능합니다."),
    ORDER_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "ORDER-007", "이미 구매확정된 주문입니다."),
    ORDER_CONFIRM_INVALID_STATUS(HttpStatus.BAD_REQUEST, "ORDER-008", "현재 상태에서는 구매확정이 불가능합니다."),
    DELIVERY_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "DELIVERY-003", "배송 타입을 선택해 주세요."),
    DELIVERY_TYPE_ALREADY_SELECTED(HttpStatus.CONFLICT, "DELIVERY-004", "이미 배송 타입이 선택되었습니다."),
  
    // ========== LOCATION (위치 관련 에러) ==========
    INVALID_LOCATION_PARAMS(HttpStatus.BAD_REQUEST, "LOCATION-001", "위치 파라미터가 유효하지 않습니다"),
    INVALID_RADIUS(HttpStatus.BAD_REQUEST, "LOCATION-002", "검색 반경은 1, 3, 7, 10, 20, 50 중 하나여야 합니다"),
    INVALID_ADDRESS(HttpStatus.BAD_REQUEST, "LOCATION-003", "주소가 유효하지 않습니다"),
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "LOCATION-004", "입력한 주소를 찾을 수 없습니다"),
    LOCATION_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "LOCATION-005", "위치 조회 API 호출에 실패했습니다"),
    LOCATION_ENCODING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "LOCATION-006", "주소 인코딩에 실패했습니다"),
    USER_LOCATION_NOT_SET(HttpStatus.BAD_REQUEST,"LOCATION-007","설정된 위치가 없습니다. 위치를 설정 해 주세요"),
    ADDRESS_MISSMATCH(HttpStatus.BAD_REQUEST,"LOCATION-008","입력한 주소와 결과가 일치하지 않습니다. 정확한 주소를 입력해주세요."),
    SET_LOCATION_ERROR(HttpStatus.BAD_REQUEST,"LOCATION-009","위도와 경도는 필수입니다");



    private final HttpStatus status;
    private final String code;
    private final String message;
}
