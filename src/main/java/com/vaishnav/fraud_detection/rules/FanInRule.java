package com.vaishnav.fraud_detection.rules;

import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.service.GraphAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FanInRule implements FraudRule {

    private final GraphAnalysisService graphAnalysisService;

    @Override
    public RuleResult evaluate(Transaction tx) {

        if (tx.getReceiverAccountId() != null
                && graphAnalysisService
                .getFanInAccounts()
                .contains(tx.getReceiverAccountId())) {

            return RuleResult.suspicious(
                    "Fan-in check: many accounts are sending money to "
                            + tx.getReceiverAccountId() + " (possible money mule)"
            );
        }

        return RuleResult.clean();
    }

    @Override
    public int getWeight() {
        return 25;
    }

    @Override
    public String getName() {
        return "FAN_IN";
    }
}