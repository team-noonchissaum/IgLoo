package noonchissaum.backend.global.config;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auth.oauth2.handler.OAuth2FailureHandler;
import noonchissaum.backend.domain.auth.oauth2.handler.OAuth2SuccessHandler;
import noonchissaum.backend.domain.auth.oauth2.service.CustomOAuth2UserService;
import noonchissaum.backend.global.handler.JwtAccessDeniedHandler;
import noonchissaum.backend.global.handler.JwtAuthenticationEntryPoint;
import noonchissaum.backend.global.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 인증/인가 예외 처리 (JSON 응답)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )

                .authorizeHttpRequests(auth -> auth
                        //  preflight OPTIONS 허용
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        // auction,wish test
                        .requestMatchers(
                                "/api/auctions/**",
                                "/api/item/**",
                                "/ws/**"
                        ).permitAll()



                        //  actuator 전부 허용
                        .requestMatchers("/actuator/**").permitAll()
                        //인증 없이 접근
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/oauth2/login/**", //OAuth2 API진입점
                                "/health"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reports").hasAnyRole("USER", "ADMIN")//신고는 유저까지 가능
                        .requestMatchers(HttpMethod.GET, "/api/reports/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reports/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/reports/**").hasRole("ADMIN")
                        //관리자 전용
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        //OAuth2 내부 엔드포인트
                        .requestMatchers("/oauth2/authorization/**",
                                "/login/oauth2/code/**").permitAll()

                        //로그인 유저(USER,ADMIN)
                        .requestMatchers("/api/users/**").hasAnyRole("USER", "ADMIN")
                        //그 외 모두 차단
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth->oauth.authorizationEndpoint(auth->
                        auth.baseUri("/api/oauth2/login"))
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler))
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
        config.setAllowedOriginPatterns(java.util.List.of("*"));
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
