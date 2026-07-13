package com.company.shop.service;

import com.company.shop.aspect.AuditOperation;
import com.company.shop.entity.Order;
import com.company.shop.entity.Product;
import com.company.shop.exception.InsufficientStockException;
import com.company.shop.exception.ProductNotFoundException;
import com.company.shop.repository.OrderRepository;
import com.company.shop.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    @AuditOperation("ORDER_PLACED")
    public Order placeOrder(String username, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));

        if (product.getQuantity() < quantity) {
            throw new InsufficientStockException("Insufficient stock for product: " + product.getName() 
                    + ". Available: " + product.getQuantity() + ", Requested: " + quantity);
        }

        // Deduct stock
        product.setQuantity(product.getQuantity() - quantity);
        productRepository.save(product);

        // Create and save Order
        double totalAmount = product.getPrice() * quantity;
        Order order = new Order(null, username, LocalDateTime.now(), totalAmount, "PLACED");
        return orderRepository.save(order);
    }

    @Override
    public List<Order> getMyOrders(String username) {
        return orderRepository.findByUsername(username);
    }

    @Override
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + id));
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    @Transactional
    @AuditOperation("ORDER_STATUS_CHANGED")
    public Order updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + id));

        List<String> validStatuses = Arrays.asList("PLACED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED");
        if (!validStatuses.contains(status.toUpperCase())) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        order.setStatus(status.toUpperCase());
        return orderRepository.save(order);
    }
}
