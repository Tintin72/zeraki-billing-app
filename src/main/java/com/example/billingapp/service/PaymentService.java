package com.example.billingapp.service;

import com.example.billingapp.dto.PaymentRequest;
import com.example.billingapp.exception.BusinessRuleViolationException;
import com.example.billingapp.exception.ResourceNotFoundException;
import com.example.billingapp.model.Invoice;
import com.example.billingapp.model.Payment;
import com.example.billingapp.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;

    @Transactional
    public Payment create(PaymentRequest request) {
        Invoice invoice = invoiceService.findById(request.getInvoiceId());

        if (paymentRepository.existsByTransactionNumber(request.getTransactionNumber())) {
            throw new BusinessRuleViolationException("Transaction number already exists: " + request.getTransactionNumber());
        }

        double alreadyPaid = paymentRepository.sumAmountByInvoiceId(invoice.getId());
        if (alreadyPaid + request.getAmount() > invoice.getAmount()) {
            throw new BusinessRuleViolationException(
                    String.format("Payment would exceed invoice amount. Remaining balance: %.2f", invoice.getAmount() - alreadyPaid)
            );
        }

        Payment payment = Payment.builder()
                .invoice(invoice)
                .paymentDate(request.getPaymentDate())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .transactionNumber(request.getTransactionNumber())
                .build();
        Payment saved = paymentRepository.save(payment);

        invoiceService.recalculateStatus(invoice.getId());

        return saved;
    }

    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    public Payment findById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }
}
