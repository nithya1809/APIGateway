package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    @GetMapping("/fallback/products")
    public Mono<String> productServiceFallback() {
        return Mono.just("Product Service is currently unavailable. Please try again later.");
    }

    @GetMapping("/fallback/objects")
    public Mono<String> objectServiceFallback() {
        return Mono.just("Object Service is currently unavailable. Please try again later.");
    }
}
