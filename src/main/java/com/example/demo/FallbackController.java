package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controller to handle fallback responses for unavailable microservices.
 * Provides user-friendly messages when dependent services are down or unreachable.
 */
@RestController
public class FallbackController {

    /**
     * Fallback method for the Product Service.
     * Returns a Mono with a message indicating the Product Service is unavailable.
     *
     * @return a Mono containing the fallback message for Product Service
     */
    @GetMapping("/fallback/products")
    public Mono<String> productServiceFallback() {
        return Mono.just("Product Service is currently unavailable. Please try again later.");
    }

    /**
     * Fallback method for the Object Service.
     * Returns a Mono with a message indicating the Object Service is unavailable.
     *
     * @return a Mono containing the fallback message for Object Service
     */
    @GetMapping("/fallback/objects")
    public Mono<String> objectServiceFallback() {
        return Mono.just("Object Service is currently unavailable. Please try again later.");
    }
}
