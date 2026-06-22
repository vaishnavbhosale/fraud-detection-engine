package com.vaishnav.fraud_detection.rules;

import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class VelocityRule implements FraudRule {

    private final TransactionRepository transactionRepository;

    private static final int MAX_TRANSACTIONS = 5;
    private static final int WINDOW_MINUTES = 5;

    @Override
    public RuleResult evaluate(Transaction tx) {
        // 1. Calculate the cutoff time for our window
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(WINDOW_MINUTES);

        // 2. Count how many transactions occurred after that cutoff
        long count = transactionRepository.countByAccountIdAndTimestampAfter(tx.getAccountId(), windowStart);

        // 3. Evaluate the threshold
        if (count >= MAX_TRANSACTIONS) {
            String reason = String.format("Velocity exceeded: %d transactions detected in the last %d minutes",
                    count, WINDOW_MINUTES);
            return RuleResult.suspicious(reason);
        }

        // 4. Volume is within normal limits
        return RuleResult.clean();
    }
}
