package com.example.aiprojectmanager.tracking.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BudgetHealthDTO {
    private BigDecimal approvedBudget;
    private BigDecimal actualCost;
    private BigDecimal remainingBudget;
    private BigDecimal budgetUsedPercentage;
    private BigDecimal estimatedLaborCost;
    private String budgetHealth; // LOW, MEDIUM, HIGH, CRITICAL
}
