package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class CategorySubscriptionRes {
    /** 사용자가 설정한 관심 카테고리 목록 */
    private List<CategorySubscriptionItemRes> subscriptions;
}
