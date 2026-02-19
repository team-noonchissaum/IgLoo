package noonchissaum.backend.domain.notification.constants;

public class NotificationConstants {
    // Reference Types
    public static final String REF_TYPE_AUCTION = "AUCTION";
    public static final String REF_TYPE_CHATROOM = "CHATROOM";

    // Notification Messages
    public static final String MSG_AUCTION_IMMINENT = "경매 마감이 임박했습니다.";
    public static final String MSG_AUCTION_WINNER = "축하합니다! '%s' 경매에 낙찰되었습니다.";
    public static final String MSG_AUCTION_LOSER = "아쉽게도 '%s' 경매 낙찰에 실패했습니다.";
    public static final String MSG_AUCTION_SOLD = "등록하신 '%s' 물품이 판매되었습니다.";
    public static final String MSG_AUCTION_FAILED = "아쉽게도 '%s' 경매가 유찰되었습니다.";
    public static final String MSG_AUCTION_OUTBID = "누군가 더 높은 금액으로 입찰했습니다.";
    public static final String MSG_AUCTION_TEMP_BLOCKED = "'%s' 경매가 임시 차단되었습니다.";
    public static final String MSG_AUCTION_BLOCKED = "'%s' 경매가 차단되었습니다.";
    public static final String MSG_AUCTION_UNBLOCKED = "'%s' 경매가 차단 해제되었습니다.";
    public static final String MSG_CHAT_CREATED = "'%s' 님이 직거래 채팅을 시작했습니다.";

    // WebSocket Messages (Payloads)
    public static final String MSG_WS_AUCTION_ENDED_FAILED = "유찰되었습니다.";
    public static final String MSG_WS_AUCTION_ENDED_SUCCESS = "경매가 종료되었습니다.";
}
