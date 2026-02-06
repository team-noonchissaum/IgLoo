package noonchissaum.backend.domain.inquiry.dto.res;

import lombok.Builder;
import lombok.Getter;
import noonchissaum.backend.domain.inquiry.entity.Inquiry;

import java.time.LocalDateTime;

@Getter
@Builder
public class InquiryListRes {
    private Long inquiryId;
    private String email;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;

    public static InquiryListRes from(Inquiry inquiry) {
        return InquiryListRes.builder()
                .inquiryId(inquiry.getId())
                .email(inquiry.getEmail())
                .nickname(inquiry.getNickname())
                .content(inquiry.getContent().length() > 100
                        ? inquiry.getContent().substring(0, 100) + "..."
                        : inquiry.getContent())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}
