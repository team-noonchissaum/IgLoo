package noonchissaum.backend.domain.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiServiceProperties.class)
public class AiServiceConfig {
}
