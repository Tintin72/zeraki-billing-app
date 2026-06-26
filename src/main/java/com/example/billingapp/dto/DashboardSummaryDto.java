package com.example.billingapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    private long totalCustomers;
    private long totalInvoices;
    private double totalAmountInvoiced;
    private double totalAmountPaid;
    private double outstandingBalance;
}
