package com.example.aiprojectmanager.tracking.dto;

import lombok.Data;
import java.math.BigDecimal;
import com.example.aiprojectmanager.tracking.domain.CostCategory;
import java.time.LocalDate;

@Data
public class CreateCostEntryRequest {
    private Long taskId;
    private CostCategory category;
    private String description;
    private BigDecimal amount;
    private LocalDate entryDate;
}
