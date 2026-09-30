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
class GraphRuleTest {

    @Mock
    private GraphAnalysisService graphAnalysisService;   // the fake helper

    @InjectMocks
    private GraphRule rule;                              // the real rule, using the fake helper

    // Check 1 in the rule: is the merchant on the suspicious list?
    @Test
    void shouldReturnSuspiciousWhenMerchantIsSuspicious() {
        when(graphAnalysisService.getSuspiciousMerchants())
                .thenReturn(List.of("Shady Store"));

        Transaction tx = new Transaction();
        tx.setAccountId("ACC001");
        tx.setMerchant("Shady Store");

        RuleResult result = rule.evaluate(tx);

        assertTrue(result.isSuspicious());
        assertTrue(result.getReason().contains("suspicious merchant"));
    }

    // Check 2: circular transactions. We don't stub the merchant list, so the
    // fake returns an empty list and the rule moves on to check 2.
    @Test
    void shouldReturnSuspiciousWhenCircularTransactionDetected() {
        when(graphAnalysisService.hasCircularTransaction("ACC001"))
                .thenReturn(true);

        Transaction tx = new Transaction();
        tx.setAccountId("ACC001");
        tx.setMerchant("Amazon");

        RuleResult result = rule.evaluate(tx);

        assertTrue(result.isSuspicious());
        assertTrue(result.getReason().contains("circular"));
    }

    // Check 3: fan-in. Checks 1 and 2 default to "no", so the rule reaches check 3.
    @Test
    void shouldReturnSuspiciousWhenReceiverIsFanInAccount() {
        when(graphAnalysisService.getFanInAccounts())
                .thenReturn(List.of("ACC999"));

        Transaction tx = new Transaction();
        tx.setAccountId("ACC001");
        tx.setMerchant("Amazon");
        tx.setReceiverAccountId("ACC999");

        RuleResult result = rule.evaluate(tx);

        assertTrue(result.isSuspicious());
        assertTrue(result.getReason().contains("fan-in"));
    }

    // Nothing is suspicious, so there are no stubs at all.
    @Test
    void shouldReturnCleanWhenNothingIsSuspicious() {
        Transaction tx = new Transaction();
        tx.setAccountId("ACC001");
        tx.setMerchant("Amazon");
        tx.setReceiverAccountId("ACC002");

        RuleResult result = rule.evaluate(tx);

        assertFalse(result.isSuspicious());
    }

    // When receiverAccountId is null, the rule must skip the fan-in check.
    @Test
    void shouldSkipFanInCheckWhenReceiverIsNull() {
        Transaction tx = new Transaction();
        tx.setAccountId("ACC001");
        tx.setMerchant("Amazon");
        // receiverAccountId is left null on purpose

        RuleResult result = rule.evaluate(tx);

        assertFalse(result.isSuspicious());
        verify(graphAnalysisService, never()).getFanInAccounts();
    }
}
