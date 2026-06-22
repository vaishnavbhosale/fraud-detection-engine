package com.vaishnav.fraud_detection.rules;

import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CountryMismatchRule implements FraudRule {


    // @RequiredArgsConstructor automatically generates a constructor for all final fields,
    // which Spring then uses to inject the TransactionRepository.
    private final TransactionRepository transactionRepository;

    @Override
    public RuleResult evaluate(Transaction tx) {
        // 1. Fetch the most recent transaction for this account
        Optional<Transaction> previousTxOpt = transactionRepository
                .findTop1ByAccountIdOrderByTimestampDesc(tx.getAccountId());

        // 2. If there is no previous transaction, we have nothing to compare against
        if (previousTxOpt.isEmpty()) {
            return RuleResult.clean();
        }

        Transaction previousTx = previousTxOpt.get();

        // 3. Compare countries safely using .equals() (since they are likely Strings)
        if (!previousTx.getCountry().equals(tx.getCountry())) {
            String reason = String.format("Country mismatch: Previous transaction was in %s, but current is in %s",
                    previousTx.getCountry(), tx.getCountry());
            return RuleResult.suspicious(reason);
        }

        // 4. Countries match
        return RuleResult.clean();
    }
}