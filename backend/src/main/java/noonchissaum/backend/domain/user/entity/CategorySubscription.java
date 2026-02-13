package noonchissaum.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.category.entity.Category;

@Entity
@Table(
        name = "category_subscriptions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_category_subscription",
                        columnNames = {"user_id", "category_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategorySubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_subscription_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** 구독 생성 */
    private CategorySubscription(User user, Category category) {
        this.user = user;
        this.category = category;
    }

    /** 정적 생성 팩토리 */
    public static CategorySubscription of(User user, Category category) {
        return new CategorySubscription(user, category);
    }
}
