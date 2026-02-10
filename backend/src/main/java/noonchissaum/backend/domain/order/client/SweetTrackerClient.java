package noonchissaum.backend.domain.order.client;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.order.dto.shipment.res.SweetTrackerTrackingInfoRes;
import noonchissaum.backend.domain.order.properties.SweetTrackerProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 **SweetTracker(택배 추적) 외부 API를 호출하는 HTTP 클라이언트 코드
 */

@Component
@RequiredArgsConstructor
public class SweetTrackerClient {

    private final RestClient sweetTrackerRestClient;
    private final SweetTrackerProperties props;

    public SweetTrackerTrackingInfoRes trackingInfo(String carrierCode, String trackingNumber) {
        return sweetTrackerRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/trackingInfo")
                        .queryParam("t_key", props.apiKey())
                        .queryParam("t_code", carrierCode)
                        .queryParam("t_invoice", trackingNumber)
                        .build()
                )
                .retrieve()
                .body(SweetTrackerTrackingInfoRes.class);
    }
}