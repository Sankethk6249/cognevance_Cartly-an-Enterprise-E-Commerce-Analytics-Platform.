package com.cognevance.ecommerce.controller;

import com.cognevance.ecommerce.dto.Dtos.*;
import com.cognevance.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/payments") @RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class PaymentController {
    private final OrderService service;

    @PostMapping("/{orderId}/init")
    public Map<String, Object> init(@PathVariable Long orderId, Authentication a) {
        return service.initPayment(a.getName(), orderId);
    }

    @PostMapping("/{orderId}/verify")
    public OrderDto verify(@PathVariable Long orderId, @Valid @RequestBody PaymentVerifyRequest r, Authentication a) {
        return service.confirmPayment(a.getName(), orderId, r);
    }
}