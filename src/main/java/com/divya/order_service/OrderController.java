package com.divya.order_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Order order) {
        String lockKey = "lock:order:" + order.getOrderId();

        // Try to acquire lock - only succeeds if key doesn't already exist
        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(5));

        if (Boolean.FALSE.equals(lockAcquired)) {
            // Someone else is processing this exact orderId right now
            return ResponseEntity.status(409).body("Order is already being processed, try again");
        }

        try {
            Optional<Order> existing = orderRepository.findByOrderId(order.getOrderId());
            if (existing.isPresent()) {
                return ResponseEntity.ok(existing.get());
            }

            order.setStatus("CREATED");
            order.setCreatedAt(LocalDateTime.now());
            Order saved = orderRepository.save(order);
            return ResponseEntity.ok(saved);

        } finally {
            redisTemplate.delete(lockKey); // release lock no matter what
        }
    }
}