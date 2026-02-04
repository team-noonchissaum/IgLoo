package noonchissaum.backend.domain.report.handler;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.report.entity.ReportTargetType;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.handler.ReportTargetHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuctionReportHandler implements ReportTargetHandler {
    private final AuctionRepository auctionRepository;

    @Override
    public ReportTargetType getType(){
        return ReportTargetType.AUCTION;
    }

    @Override
    public void validate(Long targetId){
        if(!auctionRepository.existsById(targetId)){
            throw new CustomException(ErrorCode.NOT_FOUND_AUCTIONS);
        }
    }
}
