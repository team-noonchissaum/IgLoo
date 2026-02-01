package noonchissaum.backend.domain.wallet.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.wallet.dto.walletTransaction.res.WalletTransactionRes;
import noonchissaum.backend.domain.wallet.dto.withdrawal.res.WithdrawalRes;
import noonchissaum.backend.domain.wallet.entity.WalletTransaction;
import noonchissaum.backend.domain.wallet.service.WalletTransactionService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wallet_transactions/me")
public class WalletTransactionController {

    private final WalletTransactionService walletTransactionService;
    
    //내 출금 내역 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<WalletTransactionRes>>> myWalletTransaction(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(ApiResponse.success("내 변동사항 조회", walletTransactionService.getMyWalletTransaction(
                                userPrincipal.getUserId(), pageable)));
    }
}
