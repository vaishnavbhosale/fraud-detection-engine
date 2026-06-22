package com.vaishnav.fraud_detection.service;

import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.model.TransactionStatus;
import com.vaishnav.fraud_detection.repository.TransactionRepository;
import com.vaishnav.fraud_detection.rules.RuleEngine;
import com.vaishnav.fraud_detection.rules.RuleResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor // no need to create constructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RuleEngine ruleEngine;

    public Transaction saveTransaction(Transaction tx) {

        // 1. Run the transaction through our gauntlet of rules
        RuleResult result = ruleEngine.evaluate(tx);

        // 2. Overwrite the client's status based on our server-side evaluation
        if (result.isSuspicious()) {
            tx.setStatus(TransactionStatus.FLAGGED);

            // Optional but highly recommended: Log the reason or save it to the DB!
            // System.out.println("Flagged Transaction: " + result.getReason());
            // tx.setFraudReason(result.getReason());
        } else {
            tx.setStatus(TransactionStatus.APPROVED);
        }

        // 3. Save the definitively evaluated transaction to the database
        return transactionRepository.save(tx);
    }
}
