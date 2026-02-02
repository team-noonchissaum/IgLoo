package noonchissaum.backend.global.dto;

public enum SocketMessageType {
    BID_STARTED, //입찰
    BID_SUCCESSED,//입찰 성공
    OUTBID, //상위 입찰자 변경
    //경매 상태
    AUCTION_STARTED,
    AUCTION_EXTENDED,
    AUCTION_CANCELLED,
    AUCTION_ENDED,
    AUCTION_RESULT,
    NOTIFICATION, //알람
}
