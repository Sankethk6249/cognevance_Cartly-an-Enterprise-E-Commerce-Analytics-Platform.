package com.cognevance.ecommerce.repository;

import com.cognevance.ecommerce.model.Order;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<Order> findByStatus(Order.Status status, Pageable pageable);
    List<Order> findByCreatedAtAfterAndStatusIn(LocalDateTime from, Collection<Order.Status> statuses);

    @Query("select o.status, count(o) from Order o group by o.status")
    List<Object[]> countByStatus();

    @Query("select coalesce(sum(o.total), 0) from Order o where o.status in :st")
    BigDecimal revenue(@Param("st") Collection<Order.Status> st);

    @Query("select o.user.id, count(o), coalesce(sum(o.total), 0) from Order o where o.status in :st group by o.user.id")
    List<Object[]> spendByUser(@Param("st") Collection<Order.Status> st);
}