package noonchissaum.backend.domain.inquiry.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.inquiry.dto.req.UnblockRequestReq;
import noonchissaum.backend.domain.inquiry.dto.res.InquiryRes;
import noonchissaum.backend.domain.inquiry.service.InquiryService;
import noonchissaum.backend.global.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiry")
public class InquiryController {

    private final InquiryService inquiryService;

    /**
     * 차단 해제 요청 제출
     * POST /api/inquiry/unblock
     * 인증 불필요 (차단된 유저도 접근 가능)
     */
    @PostMapping("/unblock")
    public ResponseEntity<ApiResponse<InquiryRes>> submitUnblockRequest(
            @RequestBody @Valid UnblockRequestReq req
    ) {
        InquiryRes response = inquiryService.submitUnblockRequest(req);
        return ResponseEntity.ok(ApiResponse.success("차단 해제 요청이 접수되었습니다.", response));
    }
}

