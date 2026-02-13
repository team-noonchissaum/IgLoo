package noonchissaum.backend.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Defect {
    private String type;
    private String location;
    private String severity;
    private Double confidence;
}
