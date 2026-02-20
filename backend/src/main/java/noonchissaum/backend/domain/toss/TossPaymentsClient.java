package noonchissaum.backend.domain.toss;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.toss.dto.cancel.TossCancelReq;
import noonchissaum.backend.domain.toss.dto.cancel.TossCancelRes;
import noonchissaum.backend.domain.toss.dto.confirm.TossConfirmReq;
import noonchissaum.backend.domain.toss.dto.confirm.TossConfirmRes;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class TossPaymentsClient {

    private final TossPaymentsProperties props;
    private final RestTemplate restTemplate;

    public TossConfirmRes confirm(String paymentKey, String orderId, int amount) {
        String url = props.getBaseUrl() + "/v1/payments/confirm";
        HttpHeaders headers = defaultHeaders();
        TossConfirmReq body = new TossConfirmReq(paymentKey, orderId, amount);
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), TossConfirmRes.class)
                .getBody();
    }

    public TossCancelRes cancel(String paymentKey, String cancelReason) {
        String url = props.getBaseUrl() + "/v1/payments/" + paymentKey + "/cancel";
        HttpHeaders headers = defaultHeaders();
        TossCancelReq body = new TossCancelReq(cancelReason, null); // 전액 취소
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), TossCancelRes.class)
                .getBody();
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", basicAuthHeader(props.getSecretKey()));
        return headers;
    }

    private String basicAuthHeader(String secretKey) {
        // secretKey 뒤에 ":" 붙여 base64 인코딩
        String raw = secretKey + ":";
        String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}
