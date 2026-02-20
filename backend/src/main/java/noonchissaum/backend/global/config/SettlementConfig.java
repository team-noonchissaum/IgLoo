package noonchissaum.backend.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "platform")
public class SettlementConfig {
    private BigDecimal feeRate = new BigDecimal("0.10");
}
