package com.vaishnav.fraud_detection.rules;

import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.service.GraphAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MerchantRule implements FraudRule {

    private final GraphAnalysisService graphAnalysisService;

    @Override
    public RuleResult evaluate(Transaction tx) {

        if (graphAnalysisService
                .getSuspiciousMerchants()
                .contains(tx.getMerchant())) {

            return RuleResult.suspicious(
                    "Merchant check: " + tx.getMerchant()
                            + " was paid by many different accounts recently"
            );
        }

        return RuleResult.clean();
    }

    @Override
    public int getWeight() {
        return 15;
    }

    @Override
    public String getName() {
        return "MERCHANT";
    }
}