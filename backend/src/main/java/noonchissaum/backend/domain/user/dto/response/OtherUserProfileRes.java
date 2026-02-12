package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import noonchissaum.backend.domain.item.dto.SellerItemRes;

import java.util.List;

@Getter
@AllArgsConstructor
public class OtherUserProfileRes {

    private Long userId;
    private String nickname;
    private String profileUrl;
    private String dong;
    private List<SellerItemRes> sellerItems;
}
