package noonchissaum.backend.domain.chatbot.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScenarioSummaryRes {
    private Long scenarioId;
    private String title;
    private String description;
}
