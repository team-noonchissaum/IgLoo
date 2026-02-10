package noonchissaum.backend.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.item.service.ItemService;
import noonchissaum.backend.domain.item.dto.SellerItemRes;
import noonchissaum.backend.domain.report.dto.ReportReq;
import noonchissaum.backend.domain.report.entity.Report;
import noonchissaum.backend.domain.report.entity.ReportStatus;
import noonchissaum.backend.domain.user.dto.request.ProfileUpdateUserReq;
import noonchissaum.backend.domain.user.dto.response.*;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.report.repository.ReportRepository;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.RedisKeys;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.service.LocationService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final WalletService walletService;
    private final StringRedisTemplate redisTemplate;
    private final ItemService itemService;
    private final LocationService locationService;

    /**본인 프로필 조회*/
    public ProfileRes getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        String blockReason = user.getStatus() == UserStatus.BLOCKED ? user.getBlockReason() : null;

        return ProfileRes.of(
                user.getId(),
                user.getNickname(),
                user.getProfileUrl(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                blockReason
        );
    }

    /**다른 유저 프로필 조회*/
    public OtherUserProfileRes getOtherUserProfile(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        return new OtherUserProfileRes(
                user.getId(),
                user.getNickname(),
                user.getProfileUrl(),
                user.getItems().stream().map(SellerItemRes::from).toList()
        );
    }

    /**프로필 수정*/
    @Transactional
    public ProfileUpdateUserRes updateProfile(Long userId, ProfileUpdateUserReq request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (!user.getNickname().equals(request.getNickname())
                && userRepository.existsByNickname(request.getNickname())) {
            throw new ApiException(ErrorCode.DUPLICATE_NICKNAME);
        }

        user.updateProfile(request.getNickname(), request.getProfileUrl());

        return new ProfileUpdateUserRes(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileUrl(),
                user.getLocation(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    /**탈퇴 시도 (첫 클릭)*/
    @Transactional(readOnly = true)
    public UserDeleteAttemptRes attemptDelete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        BigDecimal balance = walletService.getCurrentBalance(userId);

        // 크레딧 없으면 확인 창 표시
        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            return UserDeleteAttemptRes.noBalance();
        }

        // 크레딧 있을 때: 첫 시도인지 확인
        boolean isFirstAttempt = isFirstDeleteAttempt(userId);

        if (isFirstAttempt) {
            // 첫 번째 시도 - 환전 권장
            markDeleteAttempt(userId);
            return UserDeleteAttemptRes.firstAttempt(balance);
        } else {
            // 두 번째 이상 - 포기 확인 필요 (공통)
            return UserDeleteAttemptRes.confirmRequired(balance);
        }
    }
    /**잔액 포기하고 강제 탈퇴*/
    @Transactional
    public void userDelete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        BigDecimal balance = walletService.getCurrentBalance(userId);

        //탈퇴 처리
        user.delete();
        walletService.clearWalletCache(userId);
        clearDeleteAttempt(userId);

        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            log.warn("크레딧 {}원 포기하고 탈퇴 - userId: {}", balance, userId);
        } else {
            log.info("회원 탈퇴 완료 - userId: {}", userId);
        }
    }
    /**회원 신고*/
    @Transactional
    public void createReport(Long reporterId, ReportReq request) {
        User reporter=userRepository.findById(reporterId)
                .orElseThrow(()->new ApiException(ErrorCode.USER_NOT_FOUND));

        //중복 신고 방지
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporterId, request.getTargetType(), request.getTargetId())) {
            throw new ApiException(ErrorCode.ALREADY_REPORTED);
        }

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .build();

        reportRepository.save(report);
    }

    public User getUserByUserId(Long userId) {
        return userRepository.findById(userId).
                orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    /**탈퇴 첫 시도인지 확인*/
    public boolean isFirstDeleteAttempt(Long userId) {
        String attemptKey = RedisKeys.deleteAttemptUser(userId);
        return !Boolean.TRUE.equals(redisTemplate.hasKey(attemptKey));
    }

    /**탈퇴 시도 기록*/
    public void markDeleteAttempt(Long userId) {
        String attemptKey = RedisKeys.deleteAttemptUser(userId);
        redisTemplate.opsForValue().set(attemptKey, "1", 10, TimeUnit.MINUTES);
    }

    /**탈퇴 시도 기록 삭제*/
    public void clearDeleteAttempt(Long userId) {
        String attemptKey = RedisKeys.deleteAttemptUser(userId);
        redisTemplate.delete(attemptKey);
    }



}
