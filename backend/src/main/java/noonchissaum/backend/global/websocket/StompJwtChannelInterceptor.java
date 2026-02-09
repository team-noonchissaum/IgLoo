package noonchissaum.backend.global.websocket;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import noonchissaum.backend.global.security.JwtTokenProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import noonchissaum.backend.domain.chat.repository.ChatRoomRepository;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final ChatRoomRepository chatRoomRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;


        //CONNECT 프레임에서만 확인
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String raw = accessor.getFirstNativeHeader("Authorization");
            String token = resolveBearer(raw);

            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException("Missing Authorization header (STOMP CONNECT)");
            }

            if (!jwtTokenProvider.validateToken(token)) {
                throw new IllegalArgumentException("Invalid JWT token (STOMP CONNECT)");
            }
            Long userId = jwtTokenProvider.getUserId(token);
            String role = jwtTokenProvider.getRole(token); // "USER" / "ADMIN"

            log.info("[StompInterceptor] Connection Allowed - userId: {}, role: {}", userId, role);

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
            return message;
        }
        // CONNET 이후 프레임은 인증 주체 필요
        Authentication auth = (Authentication) accessor.getUser();
        if (auth == null || auth.getName() == null) {
            throw new IllegalArgumentException("Unauthenticated STOMP session");
        }

        Long userId = Long.valueOf(auth.getName());
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        // SUBSCRIBE: /topic/chat.{roomId}
        // - ADMIN: 모든 방 구독 허용 (조회 가능)
        // - USER : buyer/seller 참가자만 허용
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String dest = accessor.getDestination(); // ex) /topic/chat.123
            Long roomId = extractRoomIdFromChatTopic(dest);

            if (roomId != null) {
                if (!isAdmin) {
                    boolean member = chatRoomRepository.isMember(roomId, userId);
                    if (!member) {
                        throw new IllegalArgumentException("채팅 구독 권한이 없습니다.");
                    }
                }
                // ADMIN은 통과
            }
            return message;
        }
        //3) SEND: 관리자 전송 불가(읽기 전용 강제)
        // ChatWsController: @MessageMapping("/chat/rooms/{roomId}/messages")
        // 실제 SEND destination: /app/chat/rooms/{roomId}/messages
        if (StompCommand.SEND.equals(accessor.getCommand())) {
            String dest = accessor.getDestination();
            if (isChatSendDestination(dest) && isAdmin) {
                throw new IllegalArgumentException("관리자는 채팅 전송이 불가합니다.");
            }
        }

        return message;
    }



    private String resolveBearer(String raw) {
        if (raw == null) return null;
        return raw.startsWith("Bearer ") ? raw.substring(7) : raw;
    }
    // /topic/chat.{roomId} 에서 roomId 파싱
    private Long extractRoomIdFromChatTopic(String dest) {
        if (dest == null) return null;

        String prefix = "/topic/chat.";
        if (!dest.startsWith(prefix)) return null;

        String idStr = dest.substring(prefix.length());
        try {
            return Long.valueOf(idStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // /app/chat/rooms/{roomId}/messages 로 들어오는 SEND만 관리자 차단
    private boolean isChatSendDestination(String dest) {
        if (dest == null) return false;
        return dest.matches("^/app/chat/rooms/\\d+/messages$");
    }

}
