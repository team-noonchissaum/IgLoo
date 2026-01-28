package noonchissaum.backend.global.config;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auth.oauth2.OAuth2SuccessHandler;
import noonchissaum.backend.global.handler.JwtAccessDeniedHandler;
import noonchissaum.backend.global.handler.JwtAuthenticationEntryPoint;
import noonchissaum.backend.global.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


/**
 * Spring Security 설정
 * - JWT 기반 인증
 * - OAuth2 소셜 로그인
 * - 인증/인가 예외 시 JSON 응답 처리
 */

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final noonchissaum.backend.domain.auth.oauth2.OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;   // ← 추가
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, OAuth2SuccessHandler oAuth2SuccessHandler) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .formLogin(form->form.disable())
                .httpBasic(basic->basic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 인증/인가 예외 처리 (JSON 응답)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )

                .authorizeHttpRequests(auth -> auth
                        //  preflight OPTIONS 허용
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                        //  actuator 전부 허용
                        .requestMatchers("/actuator/**").permitAll()
                        //인증 없이 접근
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/oauth2/login/**", //OAuth2 API진입점
                                "/health"
                        ).permitAll()
                        //관리자 전용
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")
                        //OAuth2 내부 엔드포인트
                        .requestMatchers("/oauth2/authorization/**",
                                "/login/oauth2/code/**").permitAll()

                        //로그인 유저(USER,ADMIN)
                        .requestMatchers(
                                "/api/users/**",
                                "/api/reports"
                        ).hasAnyRole("USER", "ADMIN")

                        //그 외 모두 차단
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth->oauth.authorizationEndpoint(auth->
                        auth.baseUri("/api/oauth2/login"))
                        .successHandler(oAuth2SuccessHandler))
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(java.util.List.of("http://localhost:3000"));
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
