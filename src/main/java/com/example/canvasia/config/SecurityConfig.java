package com.example.canvasia.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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

import com.example.canvasia.security.jwt.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private static final String API_POSTS_PATTERN = "/api/posts/**";
        private static final String API_COMMENTS_PATTERN = "/api/comments/**";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(Customizer.withDefaults())

                // disable csrf (use JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // disable session (stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // onfig endpoint
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/public/**",
                                "/swagger-ui/**",
                                "/swagger-ui/index.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/swagger-resources/**",
                                "/webjars/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, API_POSTS_PATTERN).authenticated()
                        .requestMatchers(HttpMethod.POST, API_COMMENTS_PATTERN).authenticated()
                        .requestMatchers(HttpMethod.PUT, API_POSTS_PATTERN).authenticated()
                        .requestMatchers(HttpMethod.PUT, API_COMMENTS_PATTERN).authenticated()
                        .requestMatchers(HttpMethod.DELETE, API_POSTS_PATTERN).authenticated()
                        .requestMatchers(HttpMethod.DELETE, API_COMMENTS_PATTERN).authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/moderation/admin/**").authenticated()
                        .requestMatchers("/api/admin/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/posts/search").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/posts/pending").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/posts/saved").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/portfolios/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/portfolios/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/portfolios/my").authenticated()
                        .requestMatchers(
                                "/api/private/**",
                                "/api/profile/**",
                                "/api/follows/**",
                                "/api/messages/**"
                        ).authenticated()
                        // WebSocket handshake endpoint — Spring Security must allow it
                        .requestMatchers("/ws/**").permitAll()
                        .anyRequest().permitAll()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                        )
                )

                // ADD JWT FILTER
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "https://canvasia-one.vercel.app",
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:3000",
                "http://127.0.0.1:3000"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}