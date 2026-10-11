package com.cognevance.ecommerce.dto;

import com.cognevance.ecommerce.model.Order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Dtos {
    public record RegisterRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Email is required") @Email(message = "Email is invalid") String email,
        @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String password) {}
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record AuthResponse(String token, String name, String email, String role) {}

    public record ItemRequest(@NotNull Long productId, @Min(value = 1, message = "Quantity must be at least 1") int quantity) {}
    public record OrderRequest(@NotEmpty(message = "Cart is empty") @Valid List<ItemRequest> items,
                               @NotBlank(message = "Shipping address is required") String shippingAddress) {}
    public record StatusRequest(@NotNull Order.Status status) {}
    public record PaymentVerifyRequest(@NotBlank String paymentId, String signature) {}

    public record ItemDto(Long productId, String name, String imageUrl, int quantity, BigDecimal price) {}
    public record OrderDto(Long id, String status, BigDecimal total, LocalDateTime createdAt, String customerName,
                           String customerEmail, String shippingAddress, String paymentRef, List<ItemDto> items) {}
    public record PageDto<T>(List<T> content, int page, int totalPages, long totalElements) {}
    public record CustomerDto(Long id, String name, String email, long orders, BigDecimal spent, LocalDateTime joined) {}
}