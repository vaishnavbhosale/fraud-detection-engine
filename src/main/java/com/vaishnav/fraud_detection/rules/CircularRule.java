package com.vaishnav.fraud_detection.rules;

import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.service.GraphAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CircularRule implements FraudRule {

    private final GraphAnalysisService graphAnalysisService;

    @Override
    public RuleResult evaluate(Transaction tx) {

        if (graphAnalysisService.hasCircularTransaction(tx.getAccountId())) {

            return RuleResult.suspicious(
                    "Circular check: money is going back and forth between accounts"
            );
        }

        return RuleResult.clean();
    }

    @Override
    public int getWeight() {
        return 40;
    }

    @Override
    public String getName() {
        return "CIRCULAR";
    }
}