package com.company.shop.controller;

import com.company.shop.entity.AuditLog;
import com.company.shop.entity.Order;
import com.company.shop.entity.Product;
import com.company.shop.repository.AuditLogRepository;
import com.company.shop.repository.OrderRepository;
import com.company.shop.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class OnlineShoppingSystemIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        auditLogRepository.deleteAll();
    }

    private String getAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }

    // --- 1. Public API Tests ---
    @Test
    public void testPublicApis_BrowseProducts() throws Exception {
        Product p1 = productRepository.save(new Product(null, "iPhone 15", "Electronics", 79999.0, 10));
        Product p2 = productRepository.save(new Product(null, "Running Shoes", "Apparel", 4999.0, 20));

        // Get All
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("iPhone 15")))
                .andExpect(jsonPath("$[1].name", is("Running Shoes")));

        // Get By Id
        mockMvc.perform(get("/products/" + p1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("iPhone 15")));

        // Get By Category
        mockMvc.perform(get("/products/category/Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("iPhone 15")));
    }

    // --- 2. Security Roles & Access Control Tests ---
    @Test
    public void testSecurity_GuestBlockedFromSecuredApis() throws Exception {
        // Customer endpoint without credentials -> 401 Unauthorized
        mockMvc.perform(get("/customer/orders"))
                .andExpect(status().isUnauthorized());

        // Admin endpoint without credentials -> 401 Unauthorized
        mockMvc.perform(get("/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testSecurity_CustomerBlockedFromAdminApis() throws Exception {
        // Customer credential calling admin endpoint -> 403 Forbidden
        mockMvc.perform(get("/admin/orders")
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("customer1", "customer123")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testSecurity_AdminAllowedOnAdminApis() throws Exception {
        mockMvc.perform(get("/admin/orders")
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("admin", "admin123")))
                .andExpect(status().isOk());
    }

    // --- 3. Customer Place Order Tests & Stock Deductions ---
    @Test
    public void testCustomer_PlaceOrder_SuccessAndStockDeduction() throws Exception {
        Product product = productRepository.save(new Product(null, "Mechanical Keyboard", "Electronics", 8000.0, 10));

        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("productId", product.getId());
        orderRequest.put("quantity", 3);

        mockMvc.perform(post("/customer/orders")
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("customer1", "customer123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.username", is("customer1")))
                .andExpect(jsonPath("$.totalAmount", is(24000.0)))
                .andExpect(jsonPath("$.status", is("PLACED")));

        // Verify stock deducted
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(7, updatedProduct.getQuantity());

        // Verify audit log created via AOP
        List<AuditLog> logs = auditLogRepository.findAll();
        assertEquals(1, logs.size());
        assertEquals("customer1", logs.get(0).getUsername());
        assertEquals("ORDER_PLACED", logs.get(0).getOperation());
    }

    @Test
    public void testCustomer_PlaceOrder_InsufficientStock() throws Exception {
        Product product = productRepository.save(new Product(null, "Mechanical Keyboard", "Electronics", 8000.0, 2));

        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("productId", product.getId());
        orderRequest.put("quantity", 5);

        mockMvc.perform(post("/customer/orders")
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("customer1", "customer123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Insufficient stock")));

        // Verify stock remains unchanged
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(2, updatedProduct.getQuantity());
    }

    @Test
    public void testCustomer_ViewOwnOrderAndDetails() throws Exception {
        Product product = productRepository.save(new Product(null, "Mouse", "Electronics", 1500.0, 10));

        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("productId", product.getId());
        orderRequest.put("quantity", 1);

        // Customer1 places order
        mockMvc.perform(post("/customer/orders")
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("customer1", "customer123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated());

        List<Order> customer1Orders = orderRepository.findByUsername("customer1");
        assertFalse(customer1Orders.isEmpty());
        Long orderId = customer1Orders.get(0).getId();

        // Customer1 views own order detail -> 200 OK
        mockMvc.perform(get("/customer/orders/" + orderId)
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("customer1", "customer123")))
                .andExpect(status().isOk());

        // Customer2 attempts to view Customer1's order -> 403 Forbidden
        mockMvc.perform(get("/customer/orders/" + orderId)
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("customer2", "customer123")))
                .andExpect(status().isForbidden());
    }

    // --- 4. Admin Product CRUD & Duplicate Validations ---
    @Test
    public void testAdmin_AddProduct_SuccessAndDuplicateCheck() throws Exception {
        Product product = new Product(null, "Gaming Laptop", "Electronics", 120000.0, 15);

        // Successful add
        mockMvc.perform(post("/admin/products")
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Gaming Laptop")));

        // Try adding duplicate name -> 400 Bad Request
        mockMvc.perform(post("/admin/products")
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Duplicate product name found")));
    }

    @Test
    public void testAdmin_ProductValidations() throws Exception {
        // Negative Price
        Product p1 = new Product(null, "A", "Category", -10.0, 10);
        mockMvc.perform(post("/admin/products")
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(p1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Price must be positive")));

        // Blank Name
        Product p2 = new Product(null, "   ", "Category", 10.0, 10);
        mockMvc.perform(post("/admin/products")
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(p2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Name cannot be blank")));
    }

    // --- 5. AOP Audits & Admin Auditing Endpoints ---
    @Test
    public void testAuditingAspect_ProductAndOrderStatusAuditing() throws Exception {
        // 1. Add Product (Audits: PRODUCT_CREATE)
        Product p = productRepository.save(new Product(null, "Table", "Furniture", 4000.0, 5));
        
        // Simulating Service calls that trigger Aspect since MockMvc configures the actual Spring context.
        // We will call the controller which triggers the service method.
        Product pNew = new Product(null, "Office Chair", "Furniture", 8000.0, 10);
        String responseContent = mockMvc.perform(post("/admin/products")
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pNew)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        
        Product savedProduct = objectMapper.readValue(responseContent, Product.class);

        // 2. Update Product (Audits: PRODUCT_UPDATE)
        savedProduct.setPrice(7500.0);
        mockMvc.perform(put("/admin/products/" + savedProduct.getId())
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(savedProduct)))
                .andExpect(status().isOk());

        // 3. Delete Product (Audits: PRODUCT_DELETE)
        mockMvc.perform(delete("/admin/products/" + savedProduct.getId())
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("admin", "admin123")))
                .andExpect(status().isOk());

        // Assert all audits logged
        List<AuditLog> logs = auditLogRepository.findAll();
        assertEquals(3, logs.size());
        
        assertTrue(logs.stream().anyMatch(l -> "PRODUCT_CREATE".equals(l.getOperation())));
        assertTrue(logs.stream().anyMatch(l -> "PRODUCT_UPDATE".equals(l.getOperation())));
        assertTrue(logs.stream().anyMatch(l -> "PRODUCT_DELETE".equals(l.getOperation())));
        
        // Verify logs endpoints
        mockMvc.perform(get("/admin/audit-logs")
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        mockMvc.perform(get("/admin/audit-logs/operation/PRODUCT_CREATE")
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    // --- 6. Admin Dashboard and Aggregate Stats ---
    @Test
    public void testAdmin_DashboardStats() throws Exception {
        productRepository.save(new Product(null, "Book A", "Books", 500.0, 10));
        productRepository.save(new Product(null, "Book B", "Books", 600.0, 10));

        // Place two orders
        Map<String, Object> req1 = new HashMap<>();
        req1.put("productId", productRepository.findAll().get(0).getId());
        req1.put("quantity", 2); // 1000.0
        
        Map<String, Object> req2 = new HashMap<>();
        req2.put("productId", productRepository.findAll().get(1).getId());
        req2.put("quantity", 1); // 600.0

        mockMvc.perform(post("/customer/orders")
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("customer1", "customer123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/customer/orders")
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("customer2", "customer123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated());

        // Verify Dashboard API
        mockMvc.perform(get("/admin/dashboard")
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts", is(2)))
                .andExpect(jsonPath("$.totalOrders", is(2)))
                .andExpect(jsonPath("$.totalCustomers", is(2)))
                .andExpect(jsonPath("$.totalRevenue", is(1600.0)));
    }
}
