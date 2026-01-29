package noonchissaum.backend.global.config;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.report.entity.ReportTargetType;
import noonchissaum.backend.global.handler.ReportTargetHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReportHandlerConfig {

    private final List<ReportTargetHandler> handlers;

    @Bean
    public Map<ReportTargetType, ReportTargetHandler> reportTargetHandlerMap() {
        return handlers.stream()
                .collect(Collectors.toMap(
                        ReportTargetHandler::getType,
                        Function.identity()
                ));
    }
}
