package com.vaishnav.fraud_detection.service;

import com.vaishnav.fraud_detection.model.AIFraudReport;
import com.vaishnav.fraud_detection.model.FraudLog;
import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.model.TransactionStatus;
import com.vaishnav.fraud_detection.repository.FraudLogRepository;
import com.vaishnav.fraud_detection.repository.TransactionRepository;
import com.vaishnav.fraud_detection.rules.RuleEngine;
import com.vaishnav.fraud_detection.rules.RuleResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.vaishnav.fraud_detection.exception.ResourceNotFoundException;
import com.vaishnav.fraud_detection.exception.InvalidTransactionException;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final FraudLogRepository fraudLogRepository;
    private final RuleEngine ruleEngine;
    private final AIAnalysisService aiAnalysisService;
    private final AlertService alertService;

    public Transaction saveTransaction(Transaction tx) {

        if (tx.getAmount() == null ||
                tx.getAmount().compareTo(BigDecimal.ZERO) <= 0) {

            throw new InvalidTransactionException(
                    "Transaction amount must be greater than 0"
            );
        }

        RuleResult result = ruleEngine.evaluate(tx);

        if (!result.isSuspicious()) {

            tx.setStatus(TransactionStatus.APPROVED);

            return transactionRepository.save(tx);
        }

        tx.setStatus(TransactionStatus.FLAGGED);

        Transaction savedTransaction = transactionRepository.save(tx);

        List<Transaction> recentTransactions =
                transactionRepository.findTop10ByAccountIdOrderByTimestampDesc(
                        savedTransaction.getAccountId()
                );

        AIFraudReport report = aiAnalysisService.analyze(
                savedTransaction,
                recentTransactions,
                result.getReason()
        );

        FraudLog fraudLog = new FraudLog();

        fraudLog.setTransaction(savedTransaction);
        fraudLog.setRiskScore(report.getRiskScore());
        fraudLog.setFraudCategory(report.getFraudCategory());
        fraudLog.setExplanation(report.getExplanation());
        fraudLog.setRecommendation(report.getRecommendation());
        fraudLog.setTriggeredRule(result.getReason());
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