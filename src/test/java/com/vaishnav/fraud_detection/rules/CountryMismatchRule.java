package com.vaishnav.fraud_detection.rules;

import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CountryMismatchRuleTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CountryMismatchRule rule;

    @Test
    void shouldReturnCleanWhenNoPreviousTransaction() {
        // tell the fake database: "this account has no history"
        when(transactionRepository.findTop1ByAccountIdOrderByTimestampDesc("ACC001"))
                .thenReturn(Optional.empty());

        Transaction tx = new Transaction();
        tx.setAccountId("ACC001");
        tx.setCountry("IN");

        RuleResult result = rule.evaluate(tx);

        assertFalse(result.isSuspicious());
    }

    @Test
    void shouldReturnCleanWhenCountryIsSame() {
        // the old transaction, which happened in India
        Transaction previousTx = new Transaction();
        previousTx.setAccountId("ACC001");
        previousTx.setCountry("IN");

        // tell the fake database: "the last transaction is previousTx"
        when(transactionRepository.findTop1ByAccountIdOrderByTimestampDesc("ACC001"))
                .thenReturn(Optional.of(previousTx));

        // the new transaction, also in India
        Transaction tx = new Transaction();
        tx.setAccountId("ACC001");
        tx.setCountry("IN");

        RuleResult result = rule.evaluate(tx);

        assertFalse(result.isSuspicious());
    }

    @Test
    void shouldReturnSuspiciousWhenCountryIsDifferent() {
        // the old transaction, which happened in India
        Transaction previousTx = new Transaction();
        previousTx.setAccountId("ACC001");
        previousTx.setCountry("IN");

        when(transactionRepository.findTop1ByAccountIdOrderByTimestampDesc("ACC001"))
                .thenReturn(Optional.of(previousTx));

        // the new transaction, in a different country
        Transaction tx = new Transaction();
        tx.setAccountId("ACC001");
        tx.setCountry("US");

        RuleResult result = rule.evaluate(tx);

        assertTrue(result.isSuspicious());
        assertEquals(
                "Country mismatch: Previous transaction was in IN, but current is in US",
                result.getReason()
        );
    }
}