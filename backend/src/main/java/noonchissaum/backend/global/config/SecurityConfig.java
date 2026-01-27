package noonchissaum.backend.global.config;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auth.oauth2.OAuth2SuccessHandler;
import noonchissaum.backend.domain.auth.oauth2.service.CustomOAuth2UserService;
import noonchissaum.backend.global.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2UserService oAuth2UserService;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, OAuth2SuccessHandler oAuth2SuccessHandler) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form->form.disable())
                .httpBasic(basic->basic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
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
}
