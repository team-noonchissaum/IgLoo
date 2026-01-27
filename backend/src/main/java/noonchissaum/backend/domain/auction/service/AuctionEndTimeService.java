package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionEndTimeService {

    private final AuctionRepository auctionRepository;

    @Transactional
    @Async
    public void changeExtended(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow();

        auction.extendIfNeeded(LocalDateTime.now());
    }
}
