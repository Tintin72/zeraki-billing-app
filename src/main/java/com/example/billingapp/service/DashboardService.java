package com.example.billingapp.service;

import com.example.billingapp.dto.DashboardSummaryDto;
import com.example.billingapp.dto.MonthlyRevenueDto;
import com.example.billingapp.dto.TopCustomerDto;
import com.example.billingapp.repository.CustomerRepository;
import com.example.billingapp.repository.InvoiceRepository;
import com.example.billingapp.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    private static final LocalDate EPOCH_DATE = LocalDate.of(2000, 1, 1);

    @Cacheable(value = "dashboard:summary", key = "#startDate + ':' + #endDate")
    public DashboardSummaryDto getSummary(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDt = (startDate != null ? startDate : EPOCH_DATE).atStartOfDay();
        LocalDateTime endDt = (endDate != null ? endDate : LocalDate.now()).atTime(23, 59, 59);
        LocalDate start = startDate != null ? startDate : EPOCH_DATE;
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        long totalCustomers = customerRepository.countByCreatedAtBetween(startDt, endDt);
        long totalInvoices = invoiceRepository.countByCreatedAtBetween(startDt, endDt);
        double totalAmountInvoiced = invoiceRepository.sumAmountByCreatedAtBetween(startDt, endDt);
        double totalAmountPaid = paymentRepository.sumAmountByPaymentDateBetween(start, end);
        double outstandingBalance = totalAmountInvoiced - totalAmountPaid;

        return new DashboardSummaryDto(totalCustomers, totalInvoices, totalAmountInvoiced, totalAmountPaid, outstandingBalance);
    }

    @Cacheable(value = "dashboard:topCustomers", key = "#startDate + ':' + #endDate")
    public List<TopCustomerDto> getTopCustomers(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : EPOCH_DATE;
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        return paymentRepository.findTopCustomersByPaymentDateBetween(start, end)
                .stream()
                .limit(5)
                .map(row -> new TopCustomerDto((String) row[0], ((Number) row[1]).doubleValue()))
                .toList();
    }

    @Cacheable(value = "dashboard:monthlyRevenue", key = "#startDate + ':' + #endDate")
    public List<MonthlyRevenueDto> getMonthlyRevenue(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : EPOCH_DATE;
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        return paymentRepository.findMonthlyRevenueBetween(start, end)
                .stream()
                .map(row -> new MonthlyRevenueDto((String) row[0], ((Number) row[1]).doubleValue()))
                .toList();
    }
}
