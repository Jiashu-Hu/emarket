package com.shopping.emarket.order.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    public record HealthStatus(String service, String status) {}

    @GetMapping("/health")
    public HealthStatus health() {
        return new HealthStatus("order", "UP");
    }
}
