package noonchissaum.backend.domain.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLocationUpdateReq {
    private String address;        // 도로명
}
