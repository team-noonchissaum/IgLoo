package noonchissaum.backend.domain.item.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WishToggleRes {
    private boolean wished;

    public static WishToggleRes of(boolean wished) {
        return new WishToggleRes(wished);
    }
}
