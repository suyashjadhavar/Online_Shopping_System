package com.company.shop.service;

import com.company.shop.repository.OrderRepository;
import com.company.shop.repository.ProductRepository;
import com.company.shop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Autowired
    public DashboardServiceImpl(ProductRepository productRepository, 
                                OrderRepository orderRepository, 
                                UserRepository userRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", productRepository.count());
        stats.put("totalOrders", orderRepository.count());
        stats.put("totalCustomers", userRepository.countByRole("CUSTOMER"));
        stats.put("totalRevenue", orderRepository.sumTotalAmount());
        return stats;
    }
}
