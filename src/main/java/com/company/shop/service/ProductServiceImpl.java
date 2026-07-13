package com.company.shop.service;

import com.company.shop.aspect.AuditOperation;
import com.company.shop.entity.Product;
import com.company.shop.exception.DuplicateProductException;
import com.company.shop.exception.ProductNotFoundException;
import com.company.shop.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @AuditOperation("PRODUCT_CREATE")
    public Product addProduct(Product product) {
        validateProduct(product);

        if (productRepository.existsByName(product.getName())) {
            throw new DuplicateProductException("Duplicate product name found: " + product.getName());
        }

        return productRepository.save(product);
    }

    @Override
    @AuditOperation("PRODUCT_UPDATE")
    public Product updateProduct(Long id, Product details) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        validateProduct(details);

        if (!existing.getName().equals(details.getName()) && productRepository.existsByName(details.getName())) {
            throw new DuplicateProductException("Duplicate product name found: " + details.getName());
        }

        existing.setName(details.getName());
        existing.setCategory(details.getCategory());
        existing.setPrice(details.getPrice());
        existing.setQuantity(details.getQuantity());

        return productRepository.save(existing);
    }

    @Override
    @AuditOperation("PRODUCT_DELETE")
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    private void validateProduct(Product product) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        if (product.getPrice() == null || product.getPrice() <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        if (product.getQuantity() == null || product.getQuantity() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }
}
