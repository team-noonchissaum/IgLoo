package noonchissaum.backend.domain.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.order.entity.ChargeCheck;
import noonchissaum.backend.domain.order.repositroy.ChargeCheckRepository;
import noonchissaum.backend.domain.task.service.TaskService;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.global.RedisKeys;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChargeCheckService {

    private final RedissonClient redissonClient;
    private final TaskService taskService;
    private final ChargeRecordService chargeRecordService;


    public void confirmCharge(Long chargeCheckId, Long userId) {
        //userLock
        RLock userLock = redissonClient.getLock(RedisKeys.userLock(userId));

        try {
            boolean locked = userLock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new ApiException(ErrorCode.CHARGE_LOCK_ACQUISITION);
            }

            // pendingUser/AsyncTask 가 조건에 맞아야 충전 진행
            if (!taskService.checkTasks(userId)) {
                throw new ApiException(ErrorCode.PENDING_TASK_EXISTS);
            }

            //DB 반영 (DB 트랜잭션 + DB 락)
            chargeRecordService.confirmChargeTx(chargeCheckId, userId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ErrorCode.CHARGE_LOCK_ACQUISITION);
        } finally {
            if (userLock.isHeldByCurrentThread()) {
                userLock.unlock();
            }
        }
    }


    public void cancelCharge(Long chargeCheckId, Long userId,String cancelReason) {
        RLock userLock = redissonClient.getLock(RedisKeys.userLock(userId));

        try{
            boolean locked = userLock.tryLock(5, 3, TimeUnit.MINUTES);
            if(!locked){
                throw new ApiException(ErrorCode.CHARGE_LOCK_ACQUISITION);
            }
            if(!taskService.checkTasks(userId)){
                throw new ApiException(ErrorCode.PENDING_TASK_EXISTS);
            }
            chargeRecordService.cancelChargeTx(chargeCheckId,userId,cancelReason);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}