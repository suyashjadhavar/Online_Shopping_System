package com.company.shop.security;

import com.company.shop.entity.User;
import com.company.shop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.findByUsername("admin").isPresent()) {
            userRepository.save(new User(null, "admin", passwordEncoder.encode("admin123"), "ADMIN"));
        }
        if (!userRepository.findByUsername("customer1").isPresent()) {
            userRepository.save(new User(null, "customer1", passwordEncoder.encode("customer123"), "CUSTOMER"));
        }
        if (!userRepository.findByUsername("customer2").isPresent()) {
            userRepository.save(new User(null, "customer2", passwordEncoder.encode("customer123"), "CUSTOMER"));
        }
    }
}
