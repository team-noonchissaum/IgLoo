package noonchissaum.backend.domain.report.handler;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.item.repository.ItemRepository;
import noonchissaum.backend.domain.report.entity.ReportTargetType;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.handler.ReportTargetHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor

public class AuctionReportHandler implements ReportTargetHandler {
    private final ItemRepository itemRepository; // 경매글 = 게시글

    @Override
    public ReportTargetType getType(){
        return ReportTargetType.AUCTION;
    }

    @Override
    public void validate(Long targetId){
        if(!itemRepository.existsById(targetId)){
            throw new CustomException(ErrorCode.ITEM_NOT_FOUND);
        }
    }
}
