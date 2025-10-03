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

@Component
public class BasicAuthGatewayFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;
    private final PasswordEncoder passwordEncoder;

    public BasicAuthGatewayFilter() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:8083")
                .build();
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

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

                    if (path.startsWith("/orders") && (isUser || isAdmin)) {
                        allowed = true;
                    }

                    if (path.startsWith("/products")) {
                        if (isAdmin) {
                            allowed = true;
                        } else if (isUser && (path.contains("/findById") || path.contains("/displayAll")
                        		||path.contains("/findByCategory")||path.contains("/findById")||path.contains("/findByName"))) {
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

    @Override
    public int getOrder() {
        return -1;
    }
}
