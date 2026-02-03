package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.dto.ws.AuctionExtendedPayload;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.global.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuctionExtensionService {
    private final AuctionRepository auctionRepository;
    private final StringRedisTemplate redisTemplate;
    private final AuctionMessageService auctionMessageService;


    /**
    * 입찰 성공 이후 호출.
    * - Auction.extendIfNeeded(now)로 DB endAt/isExtended 갱신
    * - Redis endTime/isExtended 동기화
    * - 연장 발생 시 WS AUCTION_EXTENDED 발행
    */
    @Transactional
    public boolean extension(Long auctionId){
        try{
            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> new RuntimeException("auction not found"));
            boolean extended = auction.extendIfNeeded(LocalDateTime.now());
            if (!extended) return false;

            redisTemplate.opsForValue().set(RedisKeys.auctionEndTime(auctionId), auction.getEndAt().toString());
            redisTemplate.opsForValue().set(RedisKeys.auctionIsExtended(auctionId), "true");

            // WS 이벤트 발행
            AuctionExtendedPayload payload = AuctionExtendedPayload.builder()
                    .auctionId(auctionId)
                    .endAt(auction.getEndAt())
                    .isExtended(true)
                    .extendedMinutes(3)
                    .build();
            auctionMessageService.sendAuctionExtended(auctionId, payload);

            return true;
        } catch (Exception e){
            return false;
        }
    }
}
