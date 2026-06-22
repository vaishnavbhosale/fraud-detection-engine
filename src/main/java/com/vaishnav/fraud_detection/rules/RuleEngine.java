package com.vaishnav.fraud_detection.rules;

import com.vaishnav.fraud_detection.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RuleEngine {

    // Spring automatically discovers all classes implementing FraudRule
    // that are marked with @Component and injects them here.
    private final List<FraudRule> rules;

    public RuleResult evaluate(Transaction tx) {
        for (FraudRule rule : rules) {
            RuleResult result = rule.evaluate(tx);

            // As you noted, Lombok generates isSuspicious() for boolean fields.
            // Fail-fast logic: the moment one rule flags it, we halt and return.
            if (result.isSuspicious()) {
                return result;
            }
        }

        // If it survives the gauntlet of all rules, it's safe.
        return RuleResult.clean();
    }
}