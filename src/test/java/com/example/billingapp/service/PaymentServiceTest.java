package com.example.billingapp.service;

import com.example.billingapp.dto.PaymentRequest;
import com.example.billingapp.exception.BusinessRuleViolationException;
import com.example.billingapp.exception.ResourceNotFoundException;
import com.example.billingapp.model.Customer;
import com.example.billingapp.model.Invoice;
import com.example.billingapp.model.Payment;
import com.example.billingapp.model.enums.InvoiceStatus;
import com.example.billingapp.model.enums.PaymentMethod;
import com.example.billingapp.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private PaymentService paymentService;

    private Invoice invoice;
    private Payment payment;

    @BeforeEach
    void setUp() {
        Customer customer = Customer.builder()
                .id(1L)
                .name("Jane Doe")
                .email("jane@example.com")
                .build();

        invoice = Invoice.builder()
                .id(10L)
                .customer(customer)
                .amount(1000.0)
                .dueDate(LocalDate.now().plusDays(30))
                .status(InvoiceStatus.PENDING)
                .build();

        payment = Payment.builder()
                .id(100L)
                .invoice(invoice)
                .amount(500.0)
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.MPESA)
                .transactionNumber("TXN001")
                .build();
    }

    // --- create ---

    @Test
    void create_savesPayment_andRecalculatesStatus_whenValid() {
        PaymentRequest request = buildRequest(10L, 500.0, "TXN001");

        when(invoiceService.findById(10L)).thenReturn(invoice);
        when(paymentRepository.existsByTransactionNumber("TXN001")).thenReturn(false);
        when(paymentRepository.sumAmountByInvoiceId(10L)).thenReturn(0.0);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        Payment result = paymentService.create(request);

        assertThat(result.getTransactionNumber()).isEqualTo("TXN001");
        assertThat(result.getAmount()).isEqualTo(500.0);
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceService).recalculateStatus(10L);
    }

    @Test
    void create_allowsFullPayment_exactlyMatchingInvoiceAmount() {
        PaymentRequest request = buildRequest(10L, 1000.0, "TXN-FULL");

        when(invoiceService.findById(10L)).thenReturn(invoice);
        when(paymentRepository.existsByTransactionNumber("TXN-FULL")).thenReturn(false);
        when(paymentRepository.sumAmountByInvoiceId(10L)).thenReturn(0.0);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        paymentService.create(request);

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void create_throwsBusinessRuleViolation_whenTransactionNumberAlreadyExists() {
        PaymentRequest request = buildRequest(10L, 200.0, "TXN-DUPLICATE");

        when(invoiceService.findById(10L)).thenReturn(invoice);
        when(paymentRepository.existsByTransactionNumber("TXN-DUPLICATE")).thenReturn(true);

        assertThatThrownBy(() -> paymentService.create(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Transaction number already exists");

        verify(paymentRepository, never()).save(any());
        verify(invoiceService, never()).recalculateStatus(any());
    }

    @Test
    void create_throwsBusinessRuleViolation_whenPaymentWouldExceedInvoiceAmount() {
        PaymentRequest request = buildRequest(10L, 600.0, "TXN-OVER");

        when(invoiceService.findById(10L)).thenReturn(invoice);
        when(paymentRepository.existsByTransactionNumber("TXN-OVER")).thenReturn(false);
        when(paymentRepository.sumAmountByInvoiceId(10L)).thenReturn(500.0); // 500 already paid, 600 more = 1100 > 1000

        assertThatThrownBy(() -> paymentService.create(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Remaining balance: 500.00");

        verify(paymentRepository, never()).save(any());
        verify(invoiceService, never()).recalculateStatus(any());
    }

    @Test
    void create_throwsResourceNotFound_whenInvoiceMissing() {
        PaymentRequest request = buildRequest(99L, 100.0, "TXN-X");

        when(invoiceService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Invoice not found with id: 99"));

        assertThatThrownBy(() -> paymentService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(paymentRepository, never()).save(any());
    }

    // --- findAll ---

    @Test
    void findAll_returnsAllPayments() {
        when(paymentRepository.findAll()).thenReturn(List.of(payment));

        List<Payment> result = paymentService.findAll();

        assertThat(result).hasSize(1).containsExactly(payment);
    }

    // --- findById ---

    @Test
    void findById_returnsPayment_whenFound() {
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));

        Payment result = paymentService.findById(100L);

        assertThat(result.getId()).isEqualTo(100L);
    }

    @Test
    void findById_throwsResourceNotFound_whenMissing() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- helpers ---

    private PaymentRequest buildRequest(Long invoiceId, Double amount, String txnNumber) {
        PaymentRequest request = new PaymentRequest();
        request.setInvoiceId(invoiceId);
        request.setAmount(amount);
        request.setPaymentDate(LocalDate.now());
        request.setPaymentMethod(PaymentMethod.MPESA);
        request.setTransactionNumber(txnNumber);
        return request;
    }
}
