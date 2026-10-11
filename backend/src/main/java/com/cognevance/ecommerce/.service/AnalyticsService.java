package com.cognevance.ecommerce.service;

import com.cognevance.ecommerce.dto.Dtos.CustomerDto;
import com.cognevance.ecommerce.model.*;
import com.cognevance.ecommerce.model.Order.Status;
import com.cognevance.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class AnalyticsService {
    private static final List<Status> REVENUE = List.of(Status.PAID, Status.SHIPPED, Status.DELIVERED);
    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final ProductRepository products;
    private final UserRepository users;

    public Map<String, Object> summary() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("revenue", orders.revenue(REVENUE));
        m.put("totalOrders", orders.count());
        m.put("customers", users.countByRole(User.Role.CUSTOMER));
        m.put("products", products.countByActiveTrue());

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Status s : Status.values()) byStatus.put(s.name(), 0L);
        orders.countByStatus().forEach(r -> byStatus.put(((Status) r[0]).name(), (Long) r[1]));
        m.put("ordersByStatus", byStatus);

        m.put("topProducts", items.topProducts(REVENUE, PageRequest.of(0, 5)).stream()
                .map(r -> Map.of("name", r[0], "units", r[1], "revenue", r[2])).toList());
        m.put("salesByCategory", items.salesByCategory(REVENUE).stream()
                .map(r -> Map.of("category", r[0], "revenue", r[1])).toList());

        LocalDate today = LocalDate.now();
        Map<LocalDate, BigDecimal> daily = new TreeMap<>();
        for (int i = 13; i >= 0; i--) daily.put(today.minusDays(i), BigDecimal.ZERO);
        orders.findByCreatedAtAfterAndStatusIn(today.minusDays(13).atStartOfDay(), REVENUE)
                .forEach(o -> daily.merge(o.getCreatedAt().toLocalDate(), o.getTotal(), BigDecimal::add));
        m.put("salesByDay", daily.entrySet().stream()
                .map(e -> Map.of("date", e.getKey().toString(), "revenue", e.getValue())).toList());

        m.put("lowStock", products.findByActiveTrueAndStockLessThanOrderByStockAsc(5).stream()
                .map(p -> Map.of("id", p.getId(), "name", p.getName(), "stock", p.getStock())).toList());
        return m;
    }

    public List<CustomerDto> customers() {
        Map<Long, Object[]> spend = new HashMap<>();
        orders.spendByUser(REVENUE).forEach(r -> spend.put((Long) r[0], r));
        return users.findAll().stream().filter(u -> u.getRole() == User.Role.CUSTOMER).map(u -> {
            Object[] r = spend.get(u.getId());
            return new CustomerDto(u.getId(), u.getName(), u.getEmail(), r == null ? 0 : (Long) r[1],
                    r == null ? BigDecimal.ZERO : (BigDecimal) r[2], u.getCreatedAt());
        }).toList();
    }

    public String ordersCsv() {
        StringBuilder sb = new StringBuilder("Order ID,Date,Customer,Email,Status,Total,Payment Ref\n");
        orders.findAll(Sort.by("id")).forEach(o -> sb.append(o.getId()).append(',')
                .append(o.getCreatedAt()).append(',')
                .append(csv(o.getUser().getName())).append(',')
                .append(csv(o.getUser().getEmail())).append(',')
                .append(o.getStatus()).append(',')
                .append(o.getTotal()).append(',')
                .append(csv(o.getPaymentRef())).append('\n'));
        return sb.toString();
    }

    private static String csv(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}