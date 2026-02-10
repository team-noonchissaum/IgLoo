package noonchissaum.backend.domain.order.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sweettracker")
public record SweetTrackerProperties(
        String baseUrl,
        String apiKey
) {
}
