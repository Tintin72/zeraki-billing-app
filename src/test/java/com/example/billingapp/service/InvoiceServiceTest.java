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
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private InvoiceService invoiceService;

    private Customer customer;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
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
    }

    // --- create ---

    @Test
    void create_savesAndReturnsInvoice_whenCustomerExists() {
        InvoiceRequest request = new InvoiceRequest();
        request.setCustomerId(1L);
        request.setAmount(1000.0);
        request.setDueDate(LocalDate.now().plusDays(30));

        when(customerService.findById(1L)).thenReturn(customer);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        Invoice result = invoiceService.create(request);

        assertThat(result.getAmount()).isEqualTo(1000.0);
        assertThat(result.getCustomer()).isEqualTo(customer);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void create_throwsResourceNotFound_whenCustomerMissing() {
        InvoiceRequest request = new InvoiceRequest();
        request.setCustomerId(99L);

        when(customerService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Customer not found with id: 99"));

        assertThatThrownBy(() -> invoiceService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(invoiceRepository, never()).save(any());
    }

    // --- findById ---

    @Test
    void findById_returnsInvoice_whenFound() {
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));

        Invoice result = invoiceService.findById(10L);

        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void findById_throwsResourceNotFound_whenMissing() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- update ---

    @Test
    void update_updatesAmountAndDueDate() {
        InvoiceUpdateRequest request = new InvoiceUpdateRequest();
        request.setAmount(1500.0);
        request.setDueDate(LocalDate.now().plusDays(60));

        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice result = invoiceService.update(10L, request);

        assertThat(result.getAmount()).isEqualTo(1500.0);
        assertThat(result.getDueDate()).isEqualTo(LocalDate.now().plusDays(60));
    }

    @Test
    void update_ignoresNullFields() {
        InvoiceUpdateRequest request = new InvoiceUpdateRequest();
        // both fields null — nothing should change

        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice result = invoiceService.update(10L, request);

        assertThat(result.getAmount()).isEqualTo(1000.0);
    }

    // --- delete ---

    @Test
    void delete_removesInvoice_whenNoPaymentsExist() {
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.findByInvoiceId(10L)).thenReturn(List.of());

        invoiceService.delete(10L);

        verify(invoiceRepository).delete(invoice);
    }

    @Test
    void delete_throwsBusinessRuleViolation_whenInvoiceHasPayments() {
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.findByInvoiceId(10L)).thenReturn(List.of(mock(com.example.billingapp.model.Payment.class)));

        assertThatThrownBy(() -> invoiceService.delete(10L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Cannot delete invoice that has payments");

        verify(invoiceRepository, never()).delete(any());
    }

    @Test
    void delete_throwsResourceNotFound_whenInvoiceMissing() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- recalculateStatus ---

    @Test
    void recalculateStatus_setsPending_whenNothingPaid() {
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.sumAmountByInvoiceId(10L)).thenReturn(0.0);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        invoiceService.recalculateStatus(10L);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
    }

    @Test
    void recalculateStatus_setsPaid_whenFullyPaid() {
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.sumAmountByInvoiceId(10L)).thenReturn(1000.0);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        invoiceService.recalculateStatus(10L);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void recalculateStatus_setsPartiallyPaid_whenPartiallyPaid() {
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.sumAmountByInvoiceId(10L)).thenReturn(500.0);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        invoiceService.recalculateStatus(10L);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
    }

    @Test
    void recalculateStatus_setsPaid_whenOverpaidByFloatingPoint() {
        // total paid slightly above invoice amount due to float precision — still PAID
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.sumAmountByInvoiceId(10L)).thenReturn(1000.0000001);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        invoiceService.recalculateStatus(10L);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    // --- findOverdue ---

    @Test
    void findOverdue_returnsAllOverdue_whenNoFiltersProvided() {
        invoice.setDueDate(LocalDate.now().minusDays(5));
        when(invoiceRepository.findOverdue(any(), eq(InvoiceStatus.PAID))).thenReturn(List.of(invoice));
        when(paymentRepository.sumAmountByInvoiceId(10L)).thenReturn(0.0);

        List<OverdueInvoiceDto> result = invoiceService.findOverdue(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInvoiceNumber()).isEqualTo("INV10");
        assertThat(result.get(0).getStatus()).isEqualTo("OVERDUE");
        assertThat(result.get(0).getDaysOverdue()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void findOverdue_filtersByCustomer_whenOnlyCustomerIdProvided() {
        invoice.setDueDate(LocalDate.now().minusDays(3));
        when(invoiceRepository.findOverdueByCustomer(any(), eq(InvoiceStatus.PAID), eq(1L)))
                .thenReturn(List.of(invoice));
        when(paymentRepository.sumAmountByInvoiceId(10L)).thenReturn(200.0);

        List<OverdueInvoiceDto> result = invoiceService.findOverdue(1L, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBalance()).isEqualTo(800.0);
    }

    @Test
    void findOverdue_filtersByDateRange_whenOnlyDateRangeProvided() {
        invoice.setDueDate(LocalDate.now().minusDays(10));
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);

        when(invoiceRepository.findOverdueByDateRange(any(), eq(InvoiceStatus.PAID), any(), any()))
                .thenReturn(List.of(invoice));
        when(paymentRepository.sumAmountByInvoiceId(10L)).thenReturn(0.0);

        List<OverdueInvoiceDto> result = invoiceService.findOverdue(null, start, end);

        assertThat(result).hasSize(1);
    }

    @Test
    void findOverdue_filtersByCustomerAndDateRange_whenBothProvided() {
        invoice.setDueDate(LocalDate.now().minusDays(7));
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);

        when(invoiceRepository.findOverdueByCustomerAndDateRange(any(), eq(InvoiceStatus.PAID), eq(1L), any(), any()))
                .thenReturn(List.of(invoice));
        when(paymentRepository.sumAmountByInvoiceId(10L)).thenReturn(0.0);

        List<OverdueInvoiceDto> result = invoiceService.findOverdue(1L, start, end);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerName()).isEqualTo("Jane Doe");
    }
}
