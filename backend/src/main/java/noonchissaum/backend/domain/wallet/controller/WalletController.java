package noonchissaum.backend.domain.wallet.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.wallet.dto.wallet.res.WalletRes;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;

    @GetMapping("me")
    public ResponseEntity<ApiResponse<WalletRes>> getMyWallet(
            @AuthenticationPrincipal UserPrincipal userPrincipal
            ){
        return ResponseEntity.ok(ApiResponse.success("지갑 조회 성공",walletService.getMyWallet(userPrincipal.getUserId())));
    }

}
