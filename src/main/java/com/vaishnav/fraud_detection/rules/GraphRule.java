package com.vaishnav.fraud_detection.rules;

import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.service.GraphAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GraphRule implements FraudRule {

    private final GraphAnalysisService graphAnalysisService;

    @Override
    public RuleResult evaluate(Transaction tx) {

        if (graphAnalysisService
                .getSuspiciousMerchants()
                .contains(tx.getMerchant())) {

            return RuleResult.suspicious(
                    "Graph analysis: suspicious merchant"
            );
        }

        if (graphAnalysisService
                .hasCircularTransaction(tx.getAccountId())) {

            return RuleResult.suspicious(
                    "Graph analysis: circular transactions detected"
            );
        }

        return RuleResult.clean();
    }
}