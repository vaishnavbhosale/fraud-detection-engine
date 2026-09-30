package com.vaishnav.fraud_detection.rules;

import com.vaishnav.fraud_detection.model.Transaction;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;


class AmountRuleTest {

    private final AmountRule amountRule = new AmountRule();

    @Test
    void shouldReturnSuspiciousWhenAmountExceedsThreshold() {
        Transaction tx = new Transaction();
        tx.setAmount(new BigDecimal("50001"));

        RuleResult result = amountRule.evaluate(tx);

        assertTrue(result.isSuspicious());
        assertEquals(
                "Amount ₹50001 exceeds threshold",
                result.getReason()
        );
    }

    @Test
    void shouldReturnCleanWhenAmountIsBelowThreshold() {
        Transaction tx = new Transaction();
        tx.setAmount(new BigDecimal("49999"));

        RuleResult result = amountRule.evaluate(tx);

        assertFalse(result.isSuspicious());
        assertEquals(
                "Transaction looks clean",
                result.getReason()
        );
    }


    @Test
    void shouldReturnCleanWhenAmountEqualsThreshold(){
        Transaction tx = new Transaction();
        tx.setAmount(new BigDecimal("50000"));

        RuleResult result = amountRule.evaluate(tx);

        assertFalse(result.isSuspicious());
        assertEquals(
                "Transaction looks clean",
             result.getReason()
        );
    }

    @Test
    void amountJustAboveThreshold_isSuspicious() {
        Transaction tx = new Transaction();
        tx.setAmount(new BigDecimal("50000.01"));

        RuleResult result = amountRule.evaluate(tx);

        assertTrue(result.isSuspicious());
        assertEquals(
                "Amount ₹50000.01 exceeds threshold",
                result.getReason()
        );
    }
}