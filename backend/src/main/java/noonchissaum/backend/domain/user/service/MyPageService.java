package noonchissaum.backend.domain.user.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.user.dto.response.MyPageRes;
import noonchissaum.backend.domain.user.dto.response.UserWalletRes;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    /**마이페이지 조회*/
    public MyPageRes getMyPage(Long userId) {
        User user = userRepository.findByIdWithWallet(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Wallet wallet = user.getWallet();
        if (wallet == null) {
            throw new CustomException(ErrorCode.CANNOT_FIND_WALLET);
        }

        return new MyPageRes(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileUrl(),
                wallet.getBalance()
        );
    }
    /**사용자 지갑 정보 조회*/
    public UserWalletRes getWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_WALLET));
        BigDecimal balance = wallet.getBalance();
        BigDecimal lockedBalance = wallet.getLockedBalance();
        BigDecimal totalBalance = balance.add(lockedBalance);

        return new UserWalletRes(balance, lockedBalance, totalBalance);
    }

}
