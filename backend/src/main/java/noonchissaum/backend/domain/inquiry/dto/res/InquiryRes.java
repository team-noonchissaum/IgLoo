package noonchissaum.backend.domain.inquiry.dto.res;

import lombok.Builder;
import lombok.Getter;
import noonchissaum.backend.domain.inquiry.entity.Inquiry;

import java.time.LocalDateTime;

@Getter
@Builder
public class InquiryRes {
    private Long inquiryId;
    private String email;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InquiryRes from(Inquiry inquiry) {
        return InquiryRes.builder()
                .inquiryId(inquiry.getId())
                .email(inquiry.getEmail())
                .nickname(inquiry.getNickname())
                .content(inquiry.getContent())
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .build();
    }
}
