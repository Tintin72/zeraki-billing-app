package com.example.billingapp.repository;

import com.example.billingapp.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
