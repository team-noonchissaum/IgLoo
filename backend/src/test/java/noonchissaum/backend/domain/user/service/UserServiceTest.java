package noonchissaum.backend.domain.user.service;

import noonchissaum.backend.domain.category.repository.CategoryRepository;
import noonchissaum.backend.domain.item.service.ItemService;
import noonchissaum.backend.domain.report.dto.ReportReq;
import noonchissaum.backend.domain.report.entity.ReportTargetType;
import noonchissaum.backend.domain.report.repository.ReportRepository;
import noonchissaum.backend.domain.user.dto.request.ProfileUpdateUserReq;
import noonchissaum.backend.domain.user.dto.response.OtherUserProfileRes;
import noonchissaum.backend.domain.user.dto.response.ProfileRes;
import noonchissaum.backend.domain.user.dto.response.ProfileUpdateUserRes;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.CategorySubscriptionRepository;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.service.LocationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategorySubscriptionRepository categorySubscriptionRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ItemService itemService;
    @Mock
    private LocationService locationService;

    @InjectMocks
    private UserService userService;

    /**
     * 내 프로필 조회
     */
    @Test
    void getMyProfile() {
        // given
        Long userId = 1L;

        User user = User.builder()
                .email("test@test.com")
                .nickname("user")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        user.updateProfile("user", "img.png");

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        // when
        ProfileRes result = userService.getMyProfile(userId);

        // then
        assertEquals("user", result.getNickname());
        assertEquals("test@test.com", result.getEmail());
    }

    /**
     * 내 프로필 수정
     */
    @Test
    void updateProfile() {
        // given
        Long userId = 1L;

        User user = User.builder()
                .email("test@test.com")
                .nickname("user")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        ProfileUpdateUserReq request = new ProfileUpdateUserReq();
        ReflectionTestUtils.setField(request, "nickname", "newnickname");
        ReflectionTestUtils.setField(request, "profileUrl", "new.png");

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(userRepository.existsByNickname("newnickname"))
                .willReturn(false);

        // when
        ProfileUpdateUserRes result =
                userService.updateProfile(userId, request);

        // then
        assertEquals("newnickname", result.getNickname());
    }

    /**
     * 프로필 수정 - 닉네임 중복 시 예외
     */
    @Test
    void updateProfile_duplicateNickname_throwsException() {
        // given
        Long userId = 1L;

        User user = User.builder()
                .email("test@test.com")
                .nickname("user")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        ProfileUpdateUserReq request = new ProfileUpdateUserReq();
        ReflectionTestUtils.setField(request, "nickname", "existingNickname");
        ReflectionTestUtils.setField(request, "profileUrl", "new.png");

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(userRepository.existsByNickname("existingNickname"))
                .willReturn(true);

        // when & then
        assertThrows(ApiException.class, () ->
                userService.updateProfile(userId, request));
    }

    /**
     * 유저 조회 실패 - 존재하지 않는 유저
     */
    @Test
    void getMyProfile_userNotFound_throwsException() {
        // given
        Long userId = 999L;

        given(userRepository.findById(userId))
                .willReturn(Optional.empty());

        // when & then
        assertThrows(ApiException.class, () ->
                userService.getMyProfile(userId));
    }

    /**
     * 다른 유저 프로필 조회
     */
    @Test
    void getOtherUserProfile_returnsDong() {
        // given
        Long userId = 1L;

        User user = User.builder()
                .email("test@test.com")
                .nickname("user")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        ReflectionTestUtils.setField(user, "dong", "역삼동");

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        // when
        OtherUserProfileRes result = userService.getOtherUserProfile(userId);

        // then
        assertEquals("역삼동", result.getDong());
    }

    /**
     * 신고
     */
    @Test
    void createReport() {
        // given
        Long reporterId = 1L;

        User reporter = User.builder()
                .email("test@test.com")
                .nickname("user")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        ReportReq request = new ReportReq();
        ReflectionTestUtils.setField(request, "targetType", ReportTargetType.USER);
        ReflectionTestUtils.setField(request, "targetId", 2L);
        ReflectionTestUtils.setField(request, "reason", "욕설");
        ReflectionTestUtils.setField(request, "description", "상세설명");

        given(userRepository.findById(reporterId))
                .willReturn(Optional.of(reporter));

        given(reportRepository
                .existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
                        anyLong(), any(), anyLong(), any()))
                .willReturn(false);

        // when
        userService.createReport(reporterId, request);

        // then
        verify(reportRepository, times(1)).save(any());
    }

    /**
     * 신고 - 중복 신고 시 예외
     */
    @Test
    void createReport_duplicate_throwsException() {
        // given
        Long reporterId = 1L;

        User reporter = User.builder()
                .email("test@test.com")
                .nickname("user")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        ReportReq request = new ReportReq();
        ReflectionTestUtils.setField(request, "targetType", ReportTargetType.USER);
        ReflectionTestUtils.setField(request, "targetId", 2L);
        ReflectionTestUtils.setField(request, "reason", "욕설");

        given(userRepository.findById(reporterId))
                .willReturn(Optional.of(reporter));

        given(reportRepository
                .existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
                        anyLong(), any(), anyLong(), any()))
                .willReturn(true);  // 이미 신고

        // when & then
        assertThrows(ApiException.class, () ->
                userService.createReport(reporterId, request));
    }

}