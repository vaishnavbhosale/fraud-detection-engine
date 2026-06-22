package com.vaishnav.fraud_detection.rules;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import com.vaishnav.fraud_detection.model.Transaction;

@Component
public class AmountRule implements FraudRule {

    private static final BigDecimal THRESHOLD = new BigDecimal("50000");

    @Override
    public RuleResult evaluate(Transaction tx) {
        // compareTo returns 1 if tx.getAmount() > THRESHOLD
        if (tx.getAmount().compareTo(THRESHOLD) > 0) {
            return RuleResult.suspicious("Amount ₹" + tx.getAmount() + " exceeds threshold");
        }

        return RuleResult.clean();
    }
}