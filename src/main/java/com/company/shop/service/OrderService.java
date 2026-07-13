package com.company.shop.service;

import com.company.shop.entity.Order;
import java.util.List;

public interface OrderService {
    Order placeOrder(String username, Long productId, int quantity);
    List<Order> getMyOrders(String username);
    Order getOrderById(Long id);
    List<Order> getAllOrders();
    Order updateOrderStatus(Long id, String status);
}
