package com.example.aiprojectmanager.risk.dto;

import lombok.Data;
import com.example.aiprojectmanager.risk.domain.RiskStatus;

@Data
public class UpdateRiskStatusRequest {
    private RiskStatus status;
}
