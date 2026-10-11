package com.cognevance.ecommerce.controller;

import com.cognevance.ecommerce.dto.Dtos.*;
import com.cognevance.ecommerce.model.Order;
import com.cognevance.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/orders") @RequiredArgsConstructor
public class OrderController {
    private final OrderService service;

    @PostMapping @PreAuthorize("hasRole('CUSTOMER')") @ResponseStatus(HttpStatus.CREATED)
    public OrderDto create(@Valid @RequestBody OrderRequest r, Authentication a) {
        return service.create(a.getName(), r);
    }

    @GetMapping("/my") @PreAuthorize("hasRole('CUSTOMER')")
    public List<OrderDto> mine(Authentication a) { return service.mine(a.getName()); }

    @GetMapping("/{id}")
    public OrderDto get(@PathVariable Long id, Authentication a) {
        boolean admin = a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
        return service.get(a.getName(), admin, id);
    }

    @GetMapping @PreAuthorize("hasRole('ADMIN')")
    public PageDto<OrderDto> all(@RequestParam(required = false) Order.Status status,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        return service.list(status, page, size);
    }

    @PutMapping("/{id}/status") @PreAuthorize("hasRole('ADMIN')")
    public OrderDto status(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return service.updateStatus(id, r.status());
    }

    @PutMapping("/{id}/cancel") @PreAuthorize("hasRole('CUSTOMER')")
    public OrderDto cancel(@PathVariable Long id, Authentication a) { return service.cancel(a.getName(), id); }
}