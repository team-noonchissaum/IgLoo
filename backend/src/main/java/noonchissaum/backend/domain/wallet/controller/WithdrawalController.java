package noonchissaum.backend.domain.wallet.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.wallet.dto.req.WithdrawalReq;
import noonchissaum.backend.domain.wallet.dto.res.WithdrawalRes;
import noonchissaum.backend.domain.wallet.service.WithdrawalService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/withdrawals")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    // 출금 신청
    @PostMapping
    public ResponseEntity<ApiResponse<WithdrawalRes>> request(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody WithdrawalReq req
    ) {
        WithdrawalRes withdrawalRes = withdrawalService.requestWithdrawal(userPrincipal.getUserId(), req);
        return ResponseEntity.ok(ApiResponse.success("출금 신청 완료",withdrawalRes));
    }
    
    //내 출금신청 목록 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<WithdrawalRes>>> myWithdrawals(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success("내 출금신청 목록 조회",withdrawalService.getMyWithdrawals(userPrincipal.getUserId(), pageable)));
    }

    // 출금 승인
    @PostMapping("/{withdrawalId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirm(
            @PathVariable Long withdrawalId
    ) {
        withdrawalService.confirmWithdrawal(withdrawalId);
        return ResponseEntity.ok(ApiResponse.success("출금 승인 성공"));
    }

    // 출금 거부
    @PostMapping("/{withdrawalId}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable Long withdrawalId
    ) {
        withdrawalService.rejectWithdrawal(withdrawalId);
        return ResponseEntity.ok(ApiResponse.success("출금 거부 처리"));
    }


    // 승인대기 목록 조회
    @GetMapping("/requested")
    public ResponseEntity<ApiResponse<Page<WithdrawalRes>>> requested(
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("승인 대기 목록 조회",withdrawalService.getRequestedWithdrawals(pageable)));
    }


}
