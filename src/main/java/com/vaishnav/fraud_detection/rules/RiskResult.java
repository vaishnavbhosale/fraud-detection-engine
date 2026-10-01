package com.vaishnav.fraud_detection.rules;

import com.vaishnav.fraud_detection.model.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RiskResult {

    private int totalScore;
    private List<String> ruleNames;
    private List<String> reasons;
    private TransactionStatus status;
}