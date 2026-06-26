package com.example.billingapp.service;

import com.example.billingapp.dto.InvoiceRequest;
import com.example.billingapp.dto.InvoiceUpdateRequest;
import com.example.billingapp.dto.OverdueInvoiceDto;
import com.example.billingapp.exception.BusinessRuleViolationException;
import com.example.billingapp.exception.ResourceNotFoundException;
import com.example.billingapp.model.Customer;
import com.example.billingapp.model.Invoice;
import com.example.billingapp.model.enums.InvoiceStatus;
import com.example.billingapp.repository.InvoiceRepository;
import com.example.billingapp.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerService customerService;

    @Transactional
    public Invoice create(InvoiceRequest request) {
        Customer customer = customerService.findById(request.getCustomerId());
        Invoice invoice = Invoice.builder()
                .customer(customer)
                .amount(request.getAmount())
                .dueDate(request.getDueDate())
                .build();
        return invoiceRepository.save(invoice);
    }

    public List<Invoice> findAll() {
        return invoiceRepository.findAll();
    }

    public Invoice findById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
    }

    @Transactional
    public Invoice update(Long id, InvoiceUpdateRequest request) {
        Invoice invoice = findById(id);
        if (request.getAmount() != null) invoice.setAmount(request.getAmount());
        if (request.getDueDate() != null) invoice.setDueDate(request.getDueDate());
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void delete(Long id) {
        Invoice invoice = findById(id);
        if (!paymentRepository.findByInvoiceId(id).isEmpty()) {
            throw new BusinessRuleViolationException("Cannot delete invoice that has payments");
        }
        invoiceRepository.delete(invoice);
    }

    @Transactional
    public void recalculateStatus(Long invoiceId) {
        Invoice invoice = findById(invoiceId);
        double totalPaid = paymentRepository.sumAmountByInvoiceId(invoiceId);
        if (totalPaid <= 0) {
            invoice.setStatus(InvoiceStatus.PENDING);
        } else if (totalPaid >= invoice.getAmount()) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }
        invoiceRepository.save(invoice);
    }

    public List<OverdueInvoiceDto> findOverdue(Long customerId, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDateTime startDt = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDt = endDate != null ? endDate.atTime(23, 59, 59) : null;

        List<Invoice> invoices;
        if (customerId != null && startDt != null) {
            invoices = invoiceRepository.findOverdueByCustomerAndDateRange(today, InvoiceStatus.PAID, customerId, startDt, endDt);
        } else if (customerId != null) {
            invoices = invoiceRepository.findOverdueByCustomer(today, InvoiceStatus.PAID, customerId);
        } else if (startDt != null) {
            invoices = invoiceRepository.findOverdueByDateRange(today, InvoiceStatus.PAID, startDt, endDt);
        } else {
            invoices = invoiceRepository.findOverdue(today, InvoiceStatus.PAID);
        }

        return invoices.stream().map(inv -> {
            double amountPaid = paymentRepository.sumAmountByInvoiceId(inv.getId());
            double balance = inv.getAmount() - amountPaid;
            long daysOverdue = ChronoUnit.DAYS.between(inv.getDueDate(), today);
            return new OverdueInvoiceDto(
                    "INV" + inv.getId(),
                    inv.getCustomer().getName(),
                    inv.getAmount(),
                    amountPaid,
                    balance,
                    inv.getDueDate(),
                    daysOverdue,
                    "OVERDUE"
            );
        }).toList();
    }
}
