package noonchissaum.backend.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class securityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // 공개 API
                        .requestMatchers(
                                "/auth/**",
                                "/oauth/**",
                                "/health"
                        ).permitAll()

                        // 관리자 전용
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 일반 로그인 유저
                        .requestMatchers("/users/**", "/auctions/**")
                        .hasAnyRole("USER", "ADMIN")

                        // 나머지 전부 인증 필요
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
