package com.example.billingapp.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class InvoiceUpdateRequest {

    @Positive(message = "Amount must be positive and non-zero")
    private Double amount;

    private LocalDate dueDate;
}
