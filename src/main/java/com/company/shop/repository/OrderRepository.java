package com.company.shop.repository;

import com.company.shop.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUsername(String username);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0.0) FROM Order o")
    Double sumTotalAmount();
}
