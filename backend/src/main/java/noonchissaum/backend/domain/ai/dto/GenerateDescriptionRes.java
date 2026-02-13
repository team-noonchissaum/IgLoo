package noonchissaum.backend.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.auction.dto.req.AuctionRegisterReq;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GenerateDescriptionRes {
    private String title;
    private String summary;
    private String body;
    private List<String> hashtags;
    @JsonProperty("auction_register_req")
    private AuctionRegisterReq auctionRegisterReq;
}
