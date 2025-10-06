package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configuration class for Spring WebFlux security.
 * Disables CSRF protection and configures security rules for HTTP requests.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Configures the security filter chain for HTTP requests.
     * 
     * - Disables CSRF protection.
     * - Allows unrestricted access to endpoints under /fallback/**.
     * - Permits all other exchanges without authentication.
     *
     * @param http the ServerHttpSecurity to configure
     * @return the configured SecurityWebFilterChain bean
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http.csrf().disable()
            .authorizeExchange()
            .pathMatchers("/fallback/**").permitAll()
            .anyExchange().permitAll();
        return http.build();
    }
}
