package noonchissaum.backend.global.config;
import noonchissaum.backend.domain.order.properties.SweetTrackerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(SweetTrackerProperties.class)
public class SweetTrackerConfig {
    @Bean
    public RestClient sweetTrackerRestClient(SweetTrackerProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .build();
    }
}
