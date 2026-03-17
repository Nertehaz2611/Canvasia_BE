package com.example.canvasia.config;

import org.springframework.context.annotation.*;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        )
                .filter(Authentication::isAuthenticated)
                .map(Principal::getName);
    }
}
