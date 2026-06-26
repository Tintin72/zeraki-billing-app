package com.example.billingapp.repository;

import com.example.billingapp.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByInvoiceId(Long invoiceId);

    boolean existsByTransactionNumber(String transactionNumber);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoice.id = :invoiceId")
    double sumAmountByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentDate BETWEEN :start AND :end")
    double sumAmountByPaymentDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            SELECT p.invoice.customer.name, COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.paymentDate BETWEEN :start AND :end
            GROUP BY p.invoice.customer.id, p.invoice.customer.name
            ORDER BY SUM(p.amount) DESC
            """)
    List<Object[]> findTopCustomersByPaymentDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            SELECT FUNCTION('DATE_FORMAT', p.paymentDate, '%Y-%m'), COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.paymentDate BETWEEN :start AND :end
            GROUP BY FUNCTION('DATE_FORMAT', p.paymentDate, '%Y-%m')
            ORDER BY FUNCTION('DATE_FORMAT', p.paymentDate, '%Y-%m')
            """)
    List<Object[]> findMonthlyRevenueBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
