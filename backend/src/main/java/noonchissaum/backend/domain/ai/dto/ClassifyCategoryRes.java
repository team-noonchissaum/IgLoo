package noonchissaum.backend.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClassifyCategoryRes {
    @JsonProperty("category_candidates")
    private List<CategoryCandidate> categoryCandidates;
    @JsonProperty("selected_category_id")
    private Long selectedCategoryId;
    @JsonProperty("selection_reason")
    private String selectionReason;
    @JsonProperty("needs_user_confirmation")
    private boolean needsUserConfirmation;
}
