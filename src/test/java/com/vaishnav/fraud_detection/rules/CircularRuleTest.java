package com.vaishnav.fraud_detection.rules;

import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.service.GraphAnalysisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CircularRuleTest {

    @Mock
    private GraphAnalysisService graphAnalysisService;

    @InjectMocks
    private CircularRule rule;

    @Test
    void shouldReturnSuspiciousWhenCircularTransactionExists() {
        when(graphAnalysisService.hasCircularTransaction("ACC001"))
                .thenReturn(true);

        Transaction tx = new Transaction();
        tx.setAccountId("ACC001");

        RuleResult result = rule.evaluate(tx);

        assertTrue(result.isSuspicious());
    }

    @Test
    void shouldReturnCleanWhenNoCircularTransaction() {
        when(graphAnalysisService.hasCircularTransaction("ACC001"))
                .thenReturn(false);

        Transaction tx = new Transaction();
        tx.setAccountId("ACC001");

        RuleResult result = rule.evaluate(tx);

        assertFalse(result.isSuspicious());
    }

    @Test
    void shouldHaveCorrectWeightAndName() {
        assertEquals(40, rule.getWeight());
        assertEquals("CIRCULAR", rule.getName());
    }
}