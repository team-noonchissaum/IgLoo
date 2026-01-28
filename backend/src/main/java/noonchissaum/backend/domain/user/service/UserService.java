package noonchissaum.backend.domain.user.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.report.dto.ReportReq;
import noonchissaum.backend.domain.report.entity.Report;
import noonchissaum.backend.domain.report.entity.ReportStatus;
import noonchissaum.backend.domain.user.dto.request.ProfileUpdateUserReq;
import noonchissaum.backend.domain.user.dto.response.MyPageRes;
import noonchissaum.backend.domain.user.dto.response.OtherUserProfileRes;
import noonchissaum.backend.domain.user.dto.response.ProfileRes;
import noonchissaum.backend.domain.user.dto.response.ProfileUpdateUserRes;
import noonchissaum.backend.domain.user.entity.User;

import noonchissaum.backend.domain.user.repository.ReportRepository;
import noonchissaum.backend.domain.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;

    /**
     * 본인 프로필 조회
     */

    public ProfileRes getMyProfile(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다"));
        return new ProfileRes(
                user.getId(),
                user.getNickname(),
                user.getProfileUrl(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name()
        );
    }

    /**
     * 마이페이지 조회
     */

    public MyPageRes getMyPage(Long userId) {
        User user = userRepository.findByIdWithWallet(userId).orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        BigDecimal balance =
                user.getWallet() != null ? user.getWallet().getBalance() : BigDecimal.ZERO;

        return new MyPageRes(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileUrl(),
                balance
        );
    }

    /**
     * 다른 유저 프로필 조회
     */

    public OtherUserProfileRes getOtherUserProfile(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        return new OtherUserProfileRes(
                user.getId(),
                user.getNickname(),
                user.getProfileUrl()
//                user.getLocation()
        );
    }

    /**
     * 프로필 수정
     */

    @Transactional
    public ProfileUpdateUserRes updateProfile(Long userId, ProfileUpdateUserReq request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        if (!user.getNickname().equals(request.getNickname())
                && userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용중인 닉네임입니다.");
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

    /**
     * 회원 탈퇴
     * hard delete
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        userRepository.delete(user);

    }

    @Transactional
    public void createReport(Long reporterId, ReportReq request) {
        User reporter=userRepository.findById(reporterId)
                .orElseThrow(()->new IllegalArgumentException("유저 없음"));

        //중복 신고 방지
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporterId, request.getTargetType(), request.getTargetId())) {
            throw new IllegalStateException("이미 신고한 대상입니다.");
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
}
