package noonchissaum.backend.domain.order.dto.shipment.res;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SweetTrackerTrackingInfoRes(
        Boolean status,            // 성공/실패 (실패면 false가 오는 케이스 존재)
        String msg,                // 실패 메시지 등
        String invoiceNo,
        String senderName,
        String receiverName,
        String itemName,
        String level,              // 현재 단계(문자열)
        String complete,           // 완료 여부(Y/N 같은 값으로 오는 경우가 있음)
        List<SweetTrackerTrackingDetail> trackingDetails
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SweetTrackerTrackingDetail(
            String timeString,
            String where,
            String kind,
            String telno
    ) {}
}