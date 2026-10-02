package com.vaishnav.fraud_detection.rules;

import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.service.GraphAnalysisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FanInRuleTest {

    @Mock
    private GraphAnalysisService graphAnalysisService;

    @InjectMocks
    private FanInRule rule;

    @Test
    void shouldReturnSuspiciousWhenReceiverIsOnTheFanInList() {
        when(graphAnalysisService.getFanInAccounts())
                .thenReturn(List.of("ACC999"));

        Transaction tx = new Transaction();
        tx.setReceiverAccountId("ACC999");

        RuleResult result = rule.evaluate(tx);

        assertTrue(result.isSuspicious());
        assertTrue(result.getReason().contains("ACC999"));
    }

    @Test
    void shouldReturnCleanWhenReceiverIsNotOnTheList() {
        when(graphAnalysisService.getFanInAccounts())
                .thenReturn(List.of("ACC999"));

        Transaction tx = new Transaction();
        tx.setReceiverAccountId("ACC111");

        RuleResult result = rule.evaluate(tx);

        assertFalse(result.isSuspicious());
    }

    @Test
    void shouldSkipTheCheckWhenReceiverIsNull() {
        Transaction tx = new Transaction();
        // receiverAccountId is left null on purpose

        RuleResult result = rule.evaluate(tx);

        assertFalse(result.isSuspicious());
        verify(graphAnalysisService, never()).getFanInAccounts();
    }

    @Test
    void shouldHaveCorrectWeightAndName() {
        assertEquals(25, rule.getWeight());
        assertEquals("FAN_IN", rule.getName());
    }
}