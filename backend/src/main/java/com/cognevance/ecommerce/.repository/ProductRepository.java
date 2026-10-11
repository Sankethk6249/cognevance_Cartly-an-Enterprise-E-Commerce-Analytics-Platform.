package com.cognevance.ecommerce.repository;

import com.cognevance.ecommerce.model.Product;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("select p from Product p where p.active = true "
         + "and (:search is null or lower(p.name) like lower(concat('%', :search, '%'))) "
         + "and (:category is null or p.category = :category)")
    Page<Product> search(@Param("search") String search, @Param("category") String category, Pageable pageable);

    @Query("select distinct p.category from Product p where p.active = true order by p.category")
    List<String> categories();

    List<Product> findByActiveTrueAndStockLessThanOrderByStockAsc(int stock);
    long countByActiveTrue();
}