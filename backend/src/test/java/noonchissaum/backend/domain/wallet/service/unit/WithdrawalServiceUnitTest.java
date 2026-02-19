package noonchissaum.backend.domain.wallet.service.unit;

import noonchissaum.backend.domain.task.service.TaskService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.wallet.dto.withdrawal.req.WithdrawalReq;
import noonchissaum.backend.domain.wallet.dto.withdrawal.res.WithdrawalRes;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.entity.Withdrawal;
import noonchissaum.backend.domain.wallet.entity.WithdrawalStatus;
import noonchissaum.backend.domain.wallet.repository.WithdrawalRepository;
import noonchissaum.backend.domain.wallet.service.WithdrawalRecordService;
import noonchissaum.backend.domain.wallet.service.WithdrawalService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.util.UserLockExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class WithdrawalServiceUnitTest {

    @Mock
    private UserLockExecutor userLockExecutor;
    @Mock
    private TaskService taskService;
    @Mock
    private WithdrawalRecordService withdrawalRecordService;
    @Mock
    private WithdrawalRepository withdrawalRepository;

    @Test
    @DisplayName("pending 작업 존재 시 PENDING_TASK_EXISTS 예외 던짐")
    void requestWithdrawal_whenPendingTaskExists_throwsApiException() {
        WithdrawalService service = new WithdrawalService(userLockExecutor, taskService, withdrawalRecordService, withdrawalRepository);

        doAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        }).when(userLockExecutor).withUserLock(eq(1L), any(Supplier.class));

        when(taskService.checkTasks(1L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.requestWithdrawal(1L, new WithdrawalReq(BigDecimal.valueOf(20000), "신한", "1234")));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PENDING_TASK_EXISTS);
        verify(withdrawalRecordService, never()).requestWithdrawalTx(anyLong(), any());
    }

    @Test
    @DisplayName("출금 요청 성공 시 저장된 출금 정보 반환")
    void requestWithdrawal_success_returnsResponse() {
        WithdrawalService service = new WithdrawalService(userLockExecutor, taskService, withdrawalRecordService, withdrawalRepository);

        doAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        }).when(userLockExecutor).withUserLock(eq(2L), any(Supplier.class));

        when(taskService.checkTasks(2L)).thenReturn(true);
        when(withdrawalRecordService.requestWithdrawalTx(eq(2L), any())).thenReturn(11L);

        Withdrawal withdrawal = sampleWithdrawal(11L, 2L, WithdrawalStatus.REQUESTED);
        when(withdrawalRepository.findById(11L)).thenReturn(Optional.of(withdrawal));

        WithdrawalRes result = service.requestWithdrawal(2L, new WithdrawalReq(BigDecimal.valueOf(30000), "국민", "5678"));

        assertThat(result.withdrawalId()).isEqualTo(11L);
        assertThat(result.amount()).isEqualByComparingTo("30000");
        assertThat(result.status()).isEqualTo(WithdrawalStatus.REQUESTED);
    }

    @Test
    @DisplayName("출금 엔티티 미존재 시 WITHDRAW_NOT_FOUND 예외 던짐")
    void confirmWithdrawal_whenNotFound_throwsApiException() {
        WithdrawalService service = new WithdrawalService(userLockExecutor, taskService, withdrawalRecordService, withdrawalRepository);
        when(withdrawalRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.confirmWithdrawal(999L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WITHDRAW_NOT_FOUND);
    }

    @Test
    @DisplayName("REQUESTED 상태 출금 목록 조회 및 응답 매핑")
    void getRequestedWithdrawals_mapsResults() {
        WithdrawalService service = new WithdrawalService(userLockExecutor, taskService, withdrawalRecordService, withdrawalRepository);

        Withdrawal w1 = sampleWithdrawal(21L, 3L, WithdrawalStatus.REQUESTED);
        PageRequest pageable = PageRequest.of(0, 10);
        when(withdrawalRepository.findByStatus(WithdrawalStatus.REQUESTED, pageable))
                .thenReturn(new PageImpl<>(List.of(w1), pageable, 1));

        Page<WithdrawalRes> result = service.getRequestedWithdrawals(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).withdrawalId()).isEqualTo(21L);
        assertThat(result.getContent().get(0).status()).isEqualTo(WithdrawalStatus.REQUESTED);
    }

    private Withdrawal sampleWithdrawal(Long withdrawalId, Long userId, WithdrawalStatus status) {
        User user = User.builder()
                .email("withdraw-unit-" + userId + "@test.com")
                .nickname("withdraw_unit_" + userId)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(BigDecimal.valueOf(50000))
                .lockedBalance(BigDecimal.valueOf(0))
                .build();
        ReflectionTestUtils.setField(wallet, "id", userId + 1000);

        Withdrawal withdrawal = Withdrawal.create(wallet, BigDecimal.valueOf(30000), BigDecimal.valueOf(1000), "국민", "1234");
        ReflectionTestUtils.setField(withdrawal, "id", withdrawalId);
        ReflectionTestUtils.setField(withdrawal, "status", status);
        return withdrawal;
    }
}
