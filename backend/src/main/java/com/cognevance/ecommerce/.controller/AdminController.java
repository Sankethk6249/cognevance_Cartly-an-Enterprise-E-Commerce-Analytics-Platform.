package com.cognevance.ecommerce.controller;

import com.cognevance.ecommerce.dto.Dtos.CustomerDto;
import com.cognevance.ecommerce.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/admin") @RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AnalyticsService analytics;

    @GetMapping("/analytics")
    public Map<String, Object> summary() { return analytics.summary(); }

    @GetMapping("/customers")
    public List<CustomerDto> customers() { return analytics.customers(); }

    @GetMapping(value = "/reports/orders.csv", produces = "text/csv")
    public ResponseEntity<String> ordersCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(analytics.ordersCsv());
    }
}