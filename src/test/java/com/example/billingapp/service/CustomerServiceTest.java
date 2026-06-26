package com.example.billingapp.service;

import com.example.billingapp.dto.CustomerRequest;
import com.example.billingapp.dto.CustomerUpdateRequest;
import com.example.billingapp.exception.BusinessRuleViolationException;
import com.example.billingapp.exception.ResourceNotFoundException;
import com.example.billingapp.model.Customer;
import com.example.billingapp.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .name("Jane Doe")
                .email("jane@example.com")
                .phone("+254700000001")
                .build();
    }

    // --- create ---

    @Test
    void create_savesAndReturnsCustomer_whenEmailIsUnique() {
        CustomerRequest request = new CustomerRequest();
        request.setName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setPhone("+254700000001");

        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        Customer result = customerService.create(request);

        assertThat(result.getEmail()).isEqualTo("jane@example.com");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void create_throwsBusinessRuleViolation_whenEmailAlreadyExists() {
        CustomerRequest request = new CustomerRequest();
        request.setName("Jane Doe");
        request.setEmail("jane@example.com");

        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Email already in use");

        verify(customerRepository, never()).save(any());
    }

    // --- findAll ---

    @Test
    void findAll_returnsAllCustomers() {
        when(customerRepository.findAll()).thenReturn(List.of(customer));

        List<Customer> result = customerService.findAll();

        assertThat(result).hasSize(1).containsExactly(customer);
    }

    // --- findById ---

    @Test
    void findById_returnsCustomer_whenFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        Customer result = customerService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void findById_throwsResourceNotFound_whenMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- update ---

    @Test
    void update_updatesAllFields_whenEmailIsNewAndUnique() {
        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setName("Updated Name");
        request.setEmail("updated@example.com");
        request.setPhone("+254700000099");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmail("updated@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer result = customerService.update(1L, request);

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getEmail()).isEqualTo("updated@example.com");
        assertThat(result.getPhone()).isEqualTo("+254700000099");
    }

    @Test
    void update_doesNotCheckEmailUniqueness_whenEmailUnchanged() {
        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setEmail("jane@example.com"); // same as existing

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        customerService.update(1L, request);

        verify(customerRepository, never()).existsByEmail(any());
    }

    @Test
    void update_throwsBusinessRuleViolation_whenNewEmailTakenByAnotherCustomer() {
        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setEmail("taken@example.com");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.update(1L, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Email already in use");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void update_throwsResourceNotFound_whenCustomerMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.update(99L, new CustomerUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- delete ---

    @Test
    void delete_removesCustomer_whenFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        customerService.delete(1L);

        verify(customerRepository).delete(customer);
    }

    @Test
    void delete_throwsResourceNotFound_whenCustomerMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(customerRepository, never()).delete(any());
    }
}
