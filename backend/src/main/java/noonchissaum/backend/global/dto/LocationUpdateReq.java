package noonchissaum.backend.global.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateReq {
    private String address;  // 사용자가 입력한 주소 (필수)

    public void validate() {
        if (address == null || address.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_ADDRESS);
        }
    }
}