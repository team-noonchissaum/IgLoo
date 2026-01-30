package noonchissaum.backend.domain.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.order.dto.charge.res.ChargeCheckRes;
import noonchissaum.backend.domain.order.entity.ChargeCheck;
import noonchissaum.backend.domain.order.entity.CheckStatus;
import noonchissaum.backend.domain.order.repositroy.ChargeCheckRepository;
import noonchissaum.backend.domain.task.service.TaskService;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.global.RedisKeys;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.util.UserLockExecutor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChargeCheckService {

    private final RedissonClient redissonClient;
    private final TaskService taskService;
    private final ChargeRecordService chargeRecordService;
    private final ChargeCheckRepository chargeCheckRepository;
    private final UserLockExecutor userLockExecutor;


    public void confirmCharge(Long chargeCheckId, Long userId) {
        //userLock
        userLockExecutor.withUserLock(userId, () -> {
            //람다로 내부에 들어갈 로직 작성
            if (!taskService.checkTasks(userId)) {
                throw new ApiException(ErrorCode.PENDING_TASK_EXISTS);
            }

            chargeRecordService.confirmChargeTx(chargeCheckId, userId);
        });
    }


    public void cancelCharge(Long chargeCheckId, Long userId,String cancelReason) {
       userLockExecutor.withUserLock(userId,()->{
           if(!taskService.checkTasks(userId)){
               throw new ApiException(ErrorCode.PENDING_TASK_EXISTS);
           }
           chargeRecordService.cancelChargeTx(chargeCheckId,userId,cancelReason);
       });
    }

    @Transactional(readOnly = true)
    public List<ChargeCheckRes> getUncheckedList(Long userId) {
        return chargeCheckRepository
                .findAllByUserIdAndStatusFetchPayment(userId, CheckStatus.UNCHECKED)
                .stream()
                .map(ChargeCheckRes::from)
                .toList();
    }
}