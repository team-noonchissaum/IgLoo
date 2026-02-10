package noonchissaum.backend.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import org.springframework.messaging.simp.config.ChannelRegistration;
import noonchissaum.backend.global.websocket.StompJwtChannelInterceptor;


@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompJwtChannelInterceptor stompJwtChannelInterceptor;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompJwtChannelInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry){
        // 클라이언트가 연결하는 endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "https://ig-loo-fe-89f2.vercel.app"
                )
                .withSockJS();

    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config){
        //서버 -> 클라이언트 (브로트캐스트)
        config.enableSimpleBroker("/topic","/queue");
        //서버로 들어오는 경로
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
}
