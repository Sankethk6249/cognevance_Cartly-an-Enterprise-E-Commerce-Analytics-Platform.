package com.cognevance.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "order_items")
@Getter @Setter @NoArgsConstructor
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "order_id") private Order order;
    @ManyToOne(optional = false) @JoinColumn(name = "product_id") private Product product;
    private int quantity;
    @Column(precision = 12, scale = 2) private BigDecimal price;
}