package com.vaishnav.fraud_detection.service;


import com.vaishnav.fraud_detection.exception.InvalidTransactionException;
import com.vaishnav.fraud_detection.exception.ResourceNotFoundException;
import com.vaishnav.fraud_detection.model.AIFraudReport;
import com.vaishnav.fraud_detection.model.FraudLog;
import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.dto.TransactionRequest;
import com.vaishnav.fraud_detection.model.TransactionStatus;
import com.vaishnav.fraud_detection.repository.FraudLogRepository;
import com.vaishnav.fraud_detection.repository.TransactionRepository;
import com.vaishnav.fraud_detection.rules.RuleEngine;
import com.vaishnav.fraud_detection.rules.RuleResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.vaishnav.fraud_detection.rules.RiskResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final FraudLogRepository fraudLogRepository;
    private final RuleEngine ruleEngine;
    private final AIAnalysisService aiAnalysisService;
    private final AlertService alertService;

    private static final int LOCK_COUNT = 64;

    private final Object[] accountLocks = createLocks();

    private static Object[] createLocks() {
        Object[] locks = new Object[LOCK_COUNT];
        for (int i = 0; i < LOCK_COUNT; i++) {
            locks[i] = new Object();
        }
        return locks;
    }

    private Object lockFor(String accountId) {
        return accountLocks[Math.floorMod(accountId.hashCode(), LOCK_COUNT)];
    }

    public Transaction saveTransaction(TransactionRequest request) {

        if (request.getAmount() == null ||
                request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {

            throw new InvalidTransactionException(
                    "Transaction amount must be greater than 0"
            );
        }

        // make a new Transaction and copy only the fields the client is allowed to send
        Transaction tx = new Transaction();
        tx.setAccountId(request.getAccountId());
        tx.setReceiverAccountId(request.getReceiverAccountId());
        tx.setAmount(request.getAmount());
        tx.setCurrency(request.getCurrency());
        tx.setMerchant(request.getMerchant());
        tx.setCountry(request.getCountry());



        RiskResult risk;
        Transaction savedTransaction;

// only one request per account can be inside this block at a time
        synchronized (lockFor(request.getAccountId())) {
            // the server sets the time, not the client
            tx.setTimestamp(LocalDateTime.now());

            // count, decide and save happen together, with no gap for another request
            risk = ruleEngine.assess(tx);
            tx.setStatus(risk.getStatus());
            savedTransaction = transactionRepository.save(tx);
        }

        if (risk.getStatus() == TransactionStatus.APPROVED) {
            return savedTransaction;
        }

// join all the reasons into one text
        String reasonText = String.join("; ", risk.getReasons());

        List<Transaction> recentTransactions =
                transactionRepository.findTop10ByAccountIdOrderByTimestampDesc(
                        savedTransaction.getAccountId()
                );

        AIFraudReport report = aiAnalysisService.analyze(
                savedTransaction,
                recentTransactions,
                reasonText
        );

        FraudLog fraudLog = new FraudLog();

        fraudLog.setTransaction(savedTransaction);
        fraudLog.setRiskScore(report.getRiskScore());
        fraudLog.setFraudCategory(report.getFraudCategory());
        fraudLog.setExplanation(report.getExplanation());
        fraudLog.setRecommendation(report.getRecommendation());
        fraudLog.setTriggeredRule(reasonText);
        fraudLog.setRuleNames(String.join(",", risk.getRuleNames()));
        fraudLog.setRuleScore(risk.getTotalScore());
        fraudLog.setCreatedAt(LocalDateTime.now());

        fraudLogRepository.save(fraudLog);

        if (report.getRiskScore() >= 7) {
            alertService.sendFraudAlert(savedTransaction, fraudLog);
        }

        return savedTransaction;
    }

    public FraudLog getFraudReport(Long id) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Transaction not found with id: " + id
                        ));

        return fraudLogRepository.findByTransaction(transaction)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Fraud report not found for transaction id: " + id
                        ));
    }

}