package noonchissaum.backend.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateCategorySubscriptionReq {

    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Long categoryId;
}
