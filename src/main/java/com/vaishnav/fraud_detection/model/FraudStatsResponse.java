package com.vaishnav.fraud_detection.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudStatsResponse {

    private long totalTransactions;

    private long totalFlagged;

    private long totalApproved;

    private double flaggedPercentage;

    private Map<String, Long> fraudByRule;
}