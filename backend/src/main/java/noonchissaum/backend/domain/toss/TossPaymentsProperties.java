package noonchissaum.backend.domain.toss;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "popeye.toss")
public class TossPaymentsProperties {
    private String secretKey;
    private String baseUrl = "https://api.tosspayments.com";
}
