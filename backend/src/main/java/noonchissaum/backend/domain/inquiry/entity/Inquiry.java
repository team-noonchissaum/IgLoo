package noonchissaum.backend.domain.inquiry.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.global.entity.BaseTimeEntity;

@Entity
@Table(name = "inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    public Inquiry(String email, String nickname, String content) {
        this.email = email;
        this.nickname = nickname;
        this.content = content;
    }
}
