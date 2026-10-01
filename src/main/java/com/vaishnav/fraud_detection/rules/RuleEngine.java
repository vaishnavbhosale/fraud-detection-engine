package com.vaishnav.fraud_detection.rules;

import com.vaishnav.fraud_detection.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import com.vaishnav.fraud_detection.model.TransactionStatus;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class RuleEngine {

    // Spring automatically discovers all classes implementing FraudRule
    // that are marked with @Component and injects them here.
    private final List<FraudRule> rules;

    public RiskResult assess(Transaction tx) {
        int totalScore = 0;
        List<String> reasons = new ArrayList<>();
        List<String> ruleNames = new ArrayList<>();

        for (FraudRule rule : rules) {
            RuleResult result = rule.evaluate(tx);

            if (result.isSuspicious()) {
                totalScore += rule.getWeight();
                reasons.add(result.getReason());
                ruleNames.add(rule.getName());
            }
        }

        TransactionStatus status = decideStatus(totalScore);

        return new RiskResult(totalScore, ruleNames,reasons, status);
    }

    private TransactionStatus decideStatus(int totalScore) {
        if (totalScore >= 60) {
            return TransactionStatus.BLOCKED;
        }
        if (totalScore >= 30) {
            return TransactionStatus.FLAGGED;
        }
        return TransactionStatus.APPROVED;
    }
}
