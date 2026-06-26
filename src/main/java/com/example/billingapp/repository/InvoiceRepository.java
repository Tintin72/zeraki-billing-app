package com.example.billingapp.repository;

import com.example.billingapp.model.Invoice;
import com.example.billingapp.model.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i WHERE i.createdAt BETWEEN :start AND :end")
    double sumAmountByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT i FROM Invoice i WHERE i.dueDate < :today AND i.status <> :paid")
    List<Invoice> findOverdue(@Param("today") LocalDate today, @Param("paid") InvoiceStatus paid);

    @Query("SELECT i FROM Invoice i WHERE i.dueDate < :today AND i.status <> :paid AND i.customer.id = :customerId")
    List<Invoice> findOverdueByCustomer(@Param("today") LocalDate today, @Param("paid") InvoiceStatus paid, @Param("customerId") Long customerId);

    @Query("SELECT i FROM Invoice i WHERE i.dueDate < :today AND i.status <> :paid AND i.createdAt BETWEEN :start AND :end")
    List<Invoice> findOverdueByDateRange(@Param("today") LocalDate today, @Param("paid") InvoiceStatus paid, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT i FROM Invoice i WHERE i.dueDate < :today AND i.status <> :paid AND i.customer.id = :customerId AND i.createdAt BETWEEN :start AND :end")
    List<Invoice> findOverdueByCustomerAndDateRange(@Param("today") LocalDate today, @Param("paid") InvoiceStatus paid, @Param("customerId") Long customerId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
