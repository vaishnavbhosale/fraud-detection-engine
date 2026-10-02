package com.vaishnav.fraud_detection.rules;


import com.vaishnav.fraud_detection.model.AIFraudReport;
import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.dto.TransactionRequest;
import com.vaishnav.fraud_detection.model.TransactionStatus;
import com.vaishnav.fraud_detection.repository.FraudLogRepository;
import com.vaishnav.fraud_detection.repository.TransactionRepository;
import com.vaishnav.fraud_detection.rules.RuleEngine;
import com.vaishnav.fraud_detection.rules.RuleResult;
import com.vaishnav.fraud_detection.service.AIAnalysisService;
import com.vaishnav.fraud_detection.service.AlertService;
import com.vaishnav.fraud_detection.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FraudLogRepository fraudLogRepository;

    @Mock
    private RuleEngine ruleEngine;

    @Mock
    private AIAnalysisService aiAnalysisService;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void shouldSetTimestampOnServerAndNotSetId() {

        when(ruleEngine.assess(any(Transaction.class)))
                .thenReturn(new RiskResult(0, List.of(), List.of(), TransactionStatus.APPROVED));

        TransactionRequest request = new TransactionRequest();
        request.setAccountId("ACC001");
        request.setAmount(new BigDecimal("100"));
        request.setCurrency("INR");
        request.setMerchant("Amazon");
        request.setCountry("IN");

        transactionService.saveTransaction(request);

        // catch the Transaction that the service gave to save()
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction savedTx = captor.getValue();

        assertNull(savedTx.getId());
        assertNotNull(savedTx.getTimestamp());
    }
    private TransactionRequest makeRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setAccountId("ACC001");
        request.setAmount(new BigDecimal("95000"));
        request.setCurrency("INR");
        request.setMerchant("Amazon");
        request.setCountry("IN");
        return request;
    }

    @Test
    void shouldSendAlertWhenRulesSayBlockedEvenIfAiScoreIsLow() {
        Transaction saved = new Transaction();
        saved.setAccountId("ACC001");

        when(ruleEngine.assess(any(Transaction.class)))
                .thenReturn(new RiskResult(60, List.of("AMOUNT", "VELOCITY"),
                        List.of("reason one", "reason two"), TransactionStatus.BLOCKED));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);
        when(aiAnalysisService.analyze(any(), any(), any()))
                .thenReturn(new AIFraudReport(2, "NONE", "looks fine", "APPROVE"));

        transactionService.saveTransaction(makeRequest());

        verify(alertService).sendFraudAlert(any(), any());
    }

    @Test
    void shouldNotSendAlertWhenFlaggedAndAiScoreIsLow() {
        Transaction saved = new Transaction();
        saved.setAccountId("ACC001");

        when(ruleEngine.assess(any(Transaction.class)))
                .thenReturn(new RiskResult(30, List.of("AMOUNT"),
                        List.of("reason one"), TransactionStatus.FLAGGED));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);
        when(aiAnalysisService.analyze(any(), any(), any()))
                .thenReturn(new AIFraudReport(2, "NONE", "looks fine", "APPROVE"));

        transactionService.saveTransaction(makeRequest());

        verify(alertService, never()).sendFraudAlert(any(), any());
    }

    @Test
    void shouldSendAlertWhenFlaggedAndAiScoreIsHigh() {
        Transaction saved = new Transaction();
        saved.setAccountId("ACC001");

        when(ruleEngine.assess(any(Transaction.class)))
                .thenReturn(new RiskResult(30, List.of("AMOUNT"),
                        List.of("reason one"), TransactionStatus.FLAGGED));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);
        when(aiAnalysisService.analyze(any(), any(), any()))
                .thenReturn(new AIFraudReport(9, "FRAUD", "very risky", "BLOCK"));

        transactionService.saveTransaction(makeRequest());

        verify(alertService).sendFraudAlert(any(), any());
    }
}