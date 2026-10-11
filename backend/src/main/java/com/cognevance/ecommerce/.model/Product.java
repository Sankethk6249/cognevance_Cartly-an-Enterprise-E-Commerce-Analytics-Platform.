package com.cognevance.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_category", columnList = "category"),
        @Index(name = "idx_product_name", columnList = "name")})
@Getter @Setter @NoArgsConstructor
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Name is required") private String name;
    @NotBlank(message = "Description is required") @Column(length = 2000) private String description;
    @NotNull(message = "Price is required") @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Column(precision = 12, scale = 2) private BigDecimal price;
    @Min(value = 0, message = "Stock cannot be negative") private int stock;
    @NotBlank(message = "Category is required") private String category;
    @Column(length = 1000) private String imageUrl;
    private boolean active = true;
    @Version @JsonIgnore private Long version;
}