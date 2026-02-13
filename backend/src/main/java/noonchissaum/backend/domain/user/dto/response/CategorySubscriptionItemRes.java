package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import noonchissaum.backend.domain.user.entity.CategorySubscription;

@Getter
@AllArgsConstructor
public class CategorySubscriptionItemRes {
    private Long categoryId;
    private String categoryName;

    /** 엔티티 -> 응답 DTO 변환 */
    public static CategorySubscriptionItemRes from(CategorySubscription subscription) {
        return new CategorySubscriptionItemRes(
                subscription.getCategory().getId(),
                subscription.getCategory().getName()
        );
    }
}
