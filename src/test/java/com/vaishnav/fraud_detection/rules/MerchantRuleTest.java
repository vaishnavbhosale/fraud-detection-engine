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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantRuleTest {

    @Mock
    private GraphAnalysisService graphAnalysisService;

    @InjectMocks
    private MerchantRule rule;

    @Test
    void shouldReturnSuspiciousWhenMerchantIsOnTheList() {
        when(graphAnalysisService.getSuspiciousMerchants())
                .thenReturn(List.of("Shady Store"));

        Transaction tx = new Transaction();
        tx.setMerchant("Shady Store");

        RuleResult result = rule.evaluate(tx);

        assertTrue(result.isSuspicious());
        assertTrue(result.getReason().contains("Shady Store"));
    }

    @Test
    void shouldReturnCleanWhenMerchantIsNotOnTheList() {
        when(graphAnalysisService.getSuspiciousMerchants())
                .thenReturn(List.of("Shady Store"));

        Transaction tx = new Transaction();
        tx.setMerchant("Amazon");

        RuleResult result = rule.evaluate(tx);

        assertFalse(result.isSuspicious());
    }

    @Test
    void shouldHaveCorrectWeightAndName() {
        assertEquals(15, rule.getWeight());
        assertEquals("MERCHANT", rule.getName());
    }
}