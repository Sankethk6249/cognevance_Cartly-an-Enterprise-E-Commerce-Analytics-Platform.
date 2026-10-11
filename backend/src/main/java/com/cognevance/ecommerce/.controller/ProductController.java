package com.cognevance.ecommerce.controller;

import com.cognevance.ecommerce.dto.Dtos.PageDto;
import com.cognevance.ecommerce.model.Product;
import com.cognevance.ecommerce.repository.ProductRepository;
import com.cognevance.ecommerce.service.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.util.*;

@RestController @RequestMapping("/api/products") @RequiredArgsConstructor
public class ProductController {
    private final ProductRepository products;
    private final StorageService storage;

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    @GetMapping
    public PageDto<Product> list(@RequestParam(required = false) String search,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "12") int size,
                                 @RequestParam(defaultValue = "newest") String sort) {
        Sort s = switch (sort) {
            case "priceAsc" -> Sort.by("price").ascending();
            case "priceDesc" -> Sort.by("price").descending();
            default -> Sort.by("id").descending();
        };
        Page<Product> p = products.search(blankToNull(search), blankToNull(category),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), s));
        return new PageDto<>(p.getContent(), p.getNumber(), p.getTotalPages(), p.getTotalElements());
    }

    @GetMapping("/categories") @Cacheable("categories")
    public List<String> categories() { return products.categories(); }

    @GetMapping("/{id}")
    public Product get(@PathVariable Long id) {
        return products.findById(id).filter(Product::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @PostMapping @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.CREATED) @CacheEvict(value = "categories", allEntries = true)
    public Product create(@Valid @RequestBody Product in) {
        in.setId(null); in.setActive(true);
        return products.save(in);
    }

    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @CacheEvict(value = "categories", allEntries = true)
    public Product update(@PathVariable Long id, @Valid @RequestBody Product in) {
        Product p = get(id);
        p.setName(in.getName()); p.setDescription(in.getDescription()); p.setPrice(in.getPrice());
        p.setStock(in.getStock()); p.setCategory(in.getCategory()); p.setImageUrl(in.getImageUrl());
        return products.save(p);
    }

    /** Soft delete so past orders keep their product references. */
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @CacheEvict(value = "categories", allEntries = true)
    public void delete(@PathVariable Long id) {
        Product p = get(id);
        p.setActive(false);
        products.save(p);
    }

    @PostMapping("/upload") @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return Map.of("url", storage.store(file));
    }
}