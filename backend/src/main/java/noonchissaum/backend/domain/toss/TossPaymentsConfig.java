package noonchissaum.backend.domain.toss;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(TossPaymentsProperties.class)
public class TossPaymentsConfig {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder){
        return builder.build();
    }

}