package noonchissaum.backend.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WsMessage<T> {
    private SocketMessageType type;
    private String ts; //
    private T payload;


    public static <T> WsMessage<T> of(SocketMessageType type, T payload) {
        return new WsMessage<>(type, OffsetDateTime.now().toString(), payload);
    }

}
