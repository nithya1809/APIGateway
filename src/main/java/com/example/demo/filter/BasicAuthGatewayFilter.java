package com.example.demo.filter;

import com.example.demo.dto.UserDTO;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Global filter for basic HTTP authentication and authorization in the API Gateway.
 * This filter intercepts requests to verify the Basic Auth header,
 * validates user credentials by calling a user service,
 * checks user roles and restricts access to resources accordingly.
 */
@Component
public class BasicAuthGatewayFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;
    private final PasswordEncoder passwordEncoder;

    /**
     * Default constructor initializing WebClient and BCryptPasswordEncoder.
     * WebClient calls the user service at http://localhost:8083.
     */
    public BasicAuthGatewayFilter() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:8083")
                .build();
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Filters incoming requests to perform Basic Authentication and authorizations.
     * - Extract and decode the Basic Auth header
     * - Validate username and password against user service
     * - Authorize access based on user roles and requested path
     * - Add user info headers for downstream services
     *
     * @param exchange the current server exchange
     * @param chain    the filter chain
     * @return a Mono signaling when request processing is complete
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().add(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Gateway\"");
            return exchange.getResponse().setComplete();
        }

        String base64Creds = authHeader.substring(6);
        String credDecoded = new String(Base64.getDecoder().decode(base64Creds), StandardCharsets.UTF_8);
        String[] parts = credDecoded.split(":", 2);

        if (parts.length != 2) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String username = parts[0];
        String rawPassword = parts[1];

        return webClient.get()
                .uri("/users/{username}", username)
                .retrieve()
                .bodyToMono(UserDTO.class)
                .flatMap(userDetails -> {
                    if (userDetails == null || !passwordEncoder.matches(rawPassword, userDetails.getPassword())) {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }

                    String path = exchange.getRequest().getURI().getPath();
                    boolean isAdmin = userDetails.getRoles().contains("ROLE_ADMIN");
                    boolean isUser = userDetails.getRoles().contains("ROLE_USER");

                    boolean allowed = false;

                    // Authorization rules based on roles and request path
                    if (path.startsWith("/orders") && (isUser || isAdmin)) {
                        allowed = true;
                    }

                    if (path.startsWith("/products")) {
                        if (isAdmin) {
                            allowed = true;
                        } else if (isUser && (path.contains("/findById") || path.contains("/displayAll")
                        		|| path.contains("/findByCategory") || path.contains("/findByName"))) {
                            allowed = true;
                        }
                    }

                    if (!allowed) {
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        return exchange.getResponse().setComplete();
                    }

                    ServerHttpRequest newReq = exchange.getRequest().mutate()
                            .header("X-User", username)
                            .header("X-Roles", String.join(",", userDetails.getRoles()))
                            .build();

                    return chain.filter(exchange.mutate().request(newReq).build());
                })
                .onErrorResume(e -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
    }

    /**
     * Specifies the order of this filter.
     * Lower values have higher precedence.
     *
     * @return the order value (-1)
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
