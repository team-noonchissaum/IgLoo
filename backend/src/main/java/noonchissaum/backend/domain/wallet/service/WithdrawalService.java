package noonchissaum.backend.domain.wallet.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.task.service.TaskService;
import noonchissaum.backend.domain.wallet.dto.withdrawal.req.WithdrawalReq;
import noonchissaum.backend.domain.wallet.dto.withdrawal.res.WithdrawalRes;
import noonchissaum.backend.domain.wallet.entity.Withdrawal;
import noonchissaum.backend.domain.wallet.entity.WithdrawalStatus;
import noonchissaum.backend.domain.wallet.repository.WithdrawalRepository;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.util.UserLockExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final UserLockExecutor userLockExecutor;
    private final TaskService taskService;
    private final WithdrawalRecordService withdrawalRecordService;
    private final WithdrawalRepository withdrawalRepository;

    /**
     * 출금 승인 요청
     */

    @Transactional
    public WithdrawalRes requestWithdrawal(Long userId, WithdrawalReq req) {
        return userLockExecutor.withUserLock(userId, () -> {
            if (!taskService.checkTasks(userId)) {
                throw new ApiException(ErrorCode.PENDING_TASK_EXISTS);
            }
            Long wrId = withdrawalRecordService.requestWithdrawalTx(userId, req);
            Withdrawal withdrawal = withdrawalRepository.findById(wrId)
                    .orElseThrow(() -> new ApiException(ErrorCode.WITHDRAW_NOT_FOUND));

            return WithdrawalRes.from(withdrawal);
        });
    }

    /**
     * 내 출금 신청 목록 조회
     */

    public Page<WithdrawalRes> getMyWithdrawals(Long userId, Pageable pageable) {
        return withdrawalRepository.findByWallet_User_Id(userId, pageable)
                .map(WithdrawalRes::from);
    }

    /**
     * 출금 승인처리
     */

    @Transactional
    public void confirmWithdrawal(Long withdrawalId) {
        //
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ApiException(ErrorCode.WITHDRAW_NOT_FOUND));

        Long ownerId = withdrawal.getWallet().getUser().getId(); //
        userLockExecutor.withUserLock(ownerId, () -> { //
            if (!taskService.checkTasks(ownerId)) { //
                throw new ApiException(ErrorCode.PENDING_TASK_EXISTS);
            }

            //
            withdrawalRecordService.confirmWithdrawalTx(withdrawalId);
        });
    }

    /**
     * 출금 거부(반려)
     */

    @Transactional
    public void rejectWithdrawal(Long withdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ApiException(ErrorCode.WITHDRAW_NOT_FOUND));

        Long ownerId = withdrawal.getWallet().getUser().getId();
        userLockExecutor.withUserLock(ownerId, () -> {
            if (!taskService.checkTasks(ownerId)) {
                throw new ApiException(ErrorCode.PENDING_TASK_EXISTS);
            }
            withdrawalRecordService.rejectWithdrawalTx(withdrawalId);
        });
    }


    /**
     * 승인 대기 목록 조회
     */
    public Page<WithdrawalRes> getRequestedWithdrawals(Pageable pageable) {
        return withdrawalRepository.findByStatus(WithdrawalStatus.REQUESTED, pageable)
                .map(WithdrawalRes::from);
    }
}
