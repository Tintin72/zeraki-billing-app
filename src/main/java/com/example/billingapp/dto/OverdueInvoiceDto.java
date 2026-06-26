package com.example.billingapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class OverdueInvoiceDto {
    private String invoiceNumber;
    private String customerName;
    private double amount;
    private double amountPaid;
    private double balance;
    private LocalDate dueDate;
    private long daysOverdue;
    private String status;
}
