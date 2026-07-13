package com.company.shop.controller;

import com.company.shop.entity.AuditLog;
import com.company.shop.entity.Order;
import com.company.shop.entity.Product;
import com.company.shop.service.AuditLogService;
import com.company.shop.service.DashboardService;
import com.company.shop.service.OrderService;
import com.company.shop.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final ProductService productService;
    private final OrderService orderService;
    private final AuditLogService auditLogService;
    private final DashboardService dashboardService;

    @Autowired
    public AdminController(ProductService productService, 
                           OrderService orderService, 
                           AuditLogService auditLogService, 
                           DashboardService dashboardService) {
        this.productService = productService;
        this.orderService = orderService;
        this.auditLogService = auditLogService;
        this.dashboardService = dashboardService;
    }

    // Product Management
    @PostMapping("/products")
    public ResponseEntity<Product> addProduct(@RequestBody Product product) {
        Product createdProduct = productService.addProduct(product);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product details) {
        Product updatedProduct = productService.updateProduct(id, details);
        return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        Map<String, String> response = Map.of("message", "Product deleted successfully");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Order Management
    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        return new ResponseEntity<>(orderService.getAllOrders(), HttpStatus.OK);
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String status = request.get("status");
        Order updatedOrder = orderService.updateOrderStatus(id, status);
        return new ResponseEntity<>(updatedOrder, HttpStatus.OK);
    }

    // Audit Logging
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAllAuditLogs() {
        return new ResponseEntity<>(auditLogService.getAllAuditLogs(), HttpStatus.OK);
    }

    @GetMapping("/audit-logs/user/{username}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByUser(@PathVariable String username) {
        return new ResponseEntity<>(auditLogService.getAuditLogsByUsername(username), HttpStatus.OK);
    }

    @GetMapping("/audit-logs/operation/{operation}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByOperation(@PathVariable String operation) {
        return new ResponseEntity<>(auditLogService.getAuditLogsByOperation(operation), HttpStatus.OK);
    }

    @GetMapping("/audit-logs/reference/{id}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByReferenceId(@PathVariable Long id) {
        return new ResponseEntity<>(auditLogService.getAuditLogsByReferenceId(id), HttpStatus.OK);
    }

    // Dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return new ResponseEntity<>(dashboardService.getDashboardStats(), HttpStatus.OK);
    }
}
