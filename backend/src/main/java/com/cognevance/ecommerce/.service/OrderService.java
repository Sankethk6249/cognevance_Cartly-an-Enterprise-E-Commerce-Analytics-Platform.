package com.cognevance.ecommerce.service;

import com.cognevance.ecommerce.dto.Dtos.*;
import com.cognevance.ecommerce.model.*;
import com.cognevance.ecommerce.model.Order.Status;
import com.cognevance.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class OrderService {
    private final OrderRepository orders;
    private final ProductRepository products;
    private final UserRepository users;
    private final PaymentService payments;
    private final EmailService email;

    private User user(String email) {
        return users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Order find(Long id) {
        return orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private Order owned(String userEmail, Long id) {
        Order o = find(id);
        if (!o.getUser().getEmail().equals(userEmail))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your order");
        return o;
    }

    public OrderDto toDto(Order o) {
        List<ItemDto> items = o.getItems().stream().map(i -> new ItemDto(i.getProduct().getId(), i.getProduct().getName(),
                i.getProduct().getImageUrl(), i.getQuantity(), i.getPrice())).toList();
        return new OrderDto(o.getId(), o.getStatus().name(), o.getTotal(), o.getCreatedAt(), o.getUser().getName(),
                o.getUser().getEmail(), o.getShippingAddress(), o.getPaymentRef(), items);
    }

    public OrderDto create(String userEmail, OrderRequest r) {
        Order o = new Order();
        o.setUser(user(userEmail));
        o.setShippingAddress(r.shippingAddress().trim());
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest it : r.items()) {
            Product p = products.findById(it.productId()).filter(Product::isActive)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
            if (p.getStock() < it.quantity())
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Only " + p.getStock() + " left of " + p.getName());
            p.setStock(p.getStock() - it.quantity());
            OrderItem oi = new OrderItem();
            oi.setOrder(o); oi.setProduct(p); oi.setQuantity(it.quantity()); oi.setPrice(p.getPrice());
            o.getItems().add(oi);
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(it.quantity())));
        }
        o.setTotal(total);
        return toDto(orders.save(o));
    }

    @Transactional(readOnly = true)
    public List<OrderDto> mine(String userEmail) {
        return orders.findByUserIdOrderByCreatedAtDesc(user(userEmail).getId()).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto get(String userEmail, boolean admin, Long id) {
        return toDto(admin ? find(id) : owned(userEmail, id));
    }

    @Transactional(readOnly = true)
    public PageDto<OrderDto> list(Status status, int page, int size) {
        Pageable pg = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("id").descending());
        Page<Order> p = status == null ? orders.findAll(pg) : orders.findByStatus(status, pg);
        return new PageDto<>(p.getContent().stream().map(this::toDto).toList(), p.getNumber(), p.getTotalPages(), p.getTotalElements());
    }

    public Map<String, Object> initPayment(String userEmail, Long id) {
        Order o = owned(userEmail, id);
        if (o.getStatus() != Status.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is not awaiting payment");
        Map<String, Object> info = payments.createGatewayOrder(o);
        o.setGatewayOrderId((String) info.get("gatewayOrderId"));
        return info;
    }

    public OrderDto confirmPayment(String userEmail, Long id, PaymentVerifyRequest r) {
        Order o = owned(userEmail, id);
        if (o.getStatus() != Status.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is not awaiting payment");
        if (o.getGatewayOrderId() == null || !payments.verify(o.getGatewayOrderId(), r.paymentId(), r.signature()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment verification failed");
        o.setStatus(Status.PAID);
        o.setPaymentRef(r.paymentId());
        email.send(o.getUser().getEmail(), "Order #" + o.getId() + " confirmed",
                "Hi " + o.getUser().getName() + ",\n\nThanks for your order. We received your payment of Rs. " + o.getTotal()
                + " (ref " + r.paymentId() + ").\nWe will email you when it ships.\n\n- Cartly");
        return toDto(o);
    }

    public OrderDto cancel(String userEmail, Long id) {
        Order o = owned(userEmail, id);
        if (o.getStatus() != Status.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only unpaid orders can be cancelled");
        restoreStock(o);
        o.setStatus(Status.CANCELLED);
        return toDto(o);
    }

    public OrderDto updateStatus(Long id, Status next) {
        Order o = find(id);
        Status cur = o.getStatus();
        if (cur == Status.CANCELLED || cur == Status.DELIVERED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is already closed");
        if (next == Status.PENDING || next == Status.PAID)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose SHIPPED, DELIVERED or CANCELLED");
        if (next == Status.SHIPPED && cur != Status.PAID)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only paid orders can be shipped");
        if (next == Status.DELIVERED && cur != Status.SHIPPED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only shipped orders can be delivered");
        if (next == Status.CANCELLED) restoreStock(o);
        o.setStatus(next);
        email.send(o.getUser().getEmail(), "Order #" + o.getId() + " is now " + next,
                "Hi " + o.getUser().getName() + ",\n\nYour order #" + o.getId() + " status is now " + next + ".\n\n- Cartly");
        return toDto(o);
    }

    private void restoreStock(Order o) {
        o.getItems().forEach(i -> i.getProduct().setStock(i.getProduct().getStock() + i.getQuantity()));
    }
}