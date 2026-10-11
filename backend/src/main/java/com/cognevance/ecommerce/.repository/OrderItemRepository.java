package com.cognevance.ecommerce.repository;

import com.cognevance.ecommerce.model.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("select i.product.name, sum(i.quantity), sum(i.price * i.quantity) from OrderItem i "
         + "where i.order.status in :st group by i.product.name order by sum(i.quantity) desc")
    List<Object[]> topProducts(@Param("st") Collection<Order.Status> st, Pageable pageable);

    @Query("select i.product.category, sum(i.price * i.quantity) from OrderItem i "
         + "where i.order.status in :st group by i.product.category order by sum(i.price * i.quantity) desc")
    List<Object[]> salesByCategory(@Param("st") Collection<Order.Status> st);
}