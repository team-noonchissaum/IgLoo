package noonchissaum.backend.domain.inquiry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.inquiry.dto.req.UnblockRequestReq;
import noonchissaum.backend.domain.inquiry.dto.res.InquiryListRes;
import noonchissaum.backend.domain.inquiry.dto.res.InquiryRes;
import noonchissaum.backend.domain.inquiry.entity.Inquiry;
import noonchissaum.backend.domain.inquiry.repositroy.InquiryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    /**
     * 차단 해제 요청 제출
     */
    public InquiryRes submitUnblockRequest(UnblockRequestReq req) {
        Inquiry inquiry = Inquiry.builder()
                .email(req.getEmail())
                .nickname(req.getNickname())
                .content(req.getContent())
                .build();

        Inquiry saved = inquiryRepository.save(inquiry);
        log.info("차단 해제 요청 접수 - inquiryId: {}, email: {}, nickname: {}",
                saved.getId(), saved.getEmail(), saved.getNickname());

        return InquiryRes.from(saved);
    }

    /**
     * 문의 목록 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<InquiryListRes> getInquiries(Pageable pageable) {
        Page<Inquiry> inquiries = inquiryRepository.findAll(pageable);
        return inquiries.map(InquiryListRes::from);
    }
}
