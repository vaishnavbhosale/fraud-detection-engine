package com.vaishnav.fraud_detection.controller;

import com.vaishnav.fraud_detection.model.FraudStatsResponse;
import com.vaishnav.fraud_detection.model.TransactionStatus;
import com.vaishnav.fraud_detection.repository.FraudLogRepository;
import com.vaishnav.fraud_detection.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final TransactionRepository transactionRepository;

    private final FraudLogRepository fraudLogRepository;

    @GetMapping("/fraud-stats")
    public ResponseEntity<FraudStatsResponse> getFraudStats() {

        long totalTransactions = transactionRepository.count();

        long totalFlagged =
                transactionRepository.countByStatus(TransactionStatus.FLAGGED);

        long totalBlocked =
                transactionRepository.countByStatus(TransactionStatus.BLOCKED);

        long totalApproved =
                transactionRepository.countByStatus(TransactionStatus.APPROVED);

        double flaggedPercentage =
                totalTransactions > 0
                        ? ((totalFlagged + totalBlocked) * 100.0) / totalTransactions
                        : 0.0;

        Map<String, Long> fraudByRule = new HashMap<>();

        for (String names : fraudLogRepository.findAllRuleNames()) {
            for (String name : names.split(",")) {
                fraudByRule.put(name, fraudByRule.getOrDefault(name, 0L) + 1);
            }
        }

        FraudStatsResponse response = new FraudStatsResponse(
                totalTransactions,
                totalFlagged,
                totalBlocked,
                totalApproved,
                flaggedPercentage,
                fraudByRule
        );

        return ResponseEntity.ok(response);
    }
}