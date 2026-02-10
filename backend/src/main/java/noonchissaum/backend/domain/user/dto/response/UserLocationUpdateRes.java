package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLocationUpdateRes {
    private Double latitude;
    private Double longitude;
    private String address;
    private String jibunAddress;
    private String dong;
}
