package com.vaishnav.fraud_detection.rules;

import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VelocityRuleTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private VelocityRule velocityRule;

    @Test
    void shouldReturnCleanWhenTransactionCountIsBelowLimit() {
        Transaction tx = new Transaction();
        tx.setAccountId("account-1");

        when(transactionRepository.countByAccountIdAndTimestampAfter(
                eq("account-1"),
                any(LocalDateTime.class)
        )).thenReturn(4L);

        RuleResult result = velocityRule.evaluate(tx);

        assertFalse(result.isSuspicious());
    }

    @Test
    void shouldReturnSuspiciousWhenTransactionCountReachesLimit() {
        Transaction tx = new Transaction();
        tx.setAccountId("account-1");

        when(transactionRepository.countByAccountIdAndTimestampAfter(
                eq("account-1"),
                any(LocalDateTime.class)
        )).thenReturn(5L);

        RuleResult result = velocityRule.evaluate(tx);

        assertTrue(result.isSuspicious());
        assertEquals(
                "Velocity exceeded: 5 transactions detected in the last 5 minutes",
                result.getReason()
        );
    }

    @Test
    void shouldReturnSuspiciousWhenTransactionCountExceedsLimit() {
        Transaction tx = new Transaction();
        tx.setAccountId("account-1");

        when(transactionRepository.countByAccountIdAndTimestampAfter(
                eq("account-1"),
                any(LocalDateTime.class)
        )).thenReturn(6L);

        RuleResult result = velocityRule.evaluate(tx);

        assertTrue(result.isSuspicious());
    }
}
