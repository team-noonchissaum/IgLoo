package noonchissaum.backend.global.websocket;


import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import noonchissaum.backend.global.config.JwtTokenProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;
        //CONNECT 프레임에서만 확인
        if(StompCommand.CONNECT.equals(accessor.getCommand())){
            String raw = accessor.getFirstNativeHeader("Authorization");
            String token = resolveBearer(raw);

            if(token == null || token.isBlank()){
                throw new IllegalArgumentException("Missing Authorization header (STOMP CONNECT)");
            }
            if (!jwtTokenProvider.validateToken(token)) {
                throw new IllegalArgumentException("Invalid JWT token (STOMP CONNECT)");
            }
            Long userId = jwtTokenProvider.getUserId(token);
            String role = jwtTokenProvider.getRole(token); // "USER" / "ADMIN" 이런 값

            // Spring Security : ROLE_ prefix
            List<SimpleGrantedAuthority> authorities =
                    (role == null || role.isBlank())
                            ? List.of()
                            : List.of(new SimpleGrantedAuthority("ROLE_" + role));

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userId.toString(), // principal name ->  userId 사용
                    null,
                    authorities
            );
            accessor.setUser(auth);
        }

        return message;
    }
    private String resolveBearer(String raw) {
        if (raw == null) return null;
        return raw.startsWith("Bearer ") ? raw.substring(7) : raw;
    }

}
