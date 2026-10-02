package com.vaishnav.fraud_detection.rules;

import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.model.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleEngineTest {

    @Mock
    private FraudRule ruleA;

    @Mock
    private FraudRule ruleB;

    @Test
    void shouldAddUpWeightsWhenTwoRulesFire() {
        Transaction tx = new Transaction();

        // both fake rules say "suspicious", and each is worth 30 points
        when(ruleA.evaluate(tx)).thenReturn(RuleResult.suspicious("rule A flagged it"));
        when(ruleA.getWeight()).thenReturn(30);
        when(ruleA.getName()).thenReturn("RULE_A");
        when(ruleB.evaluate(tx)).thenReturn(RuleResult.suspicious("rule B flagged it"));
        when(ruleB.getWeight()).thenReturn(30);
        when(ruleB.getName()).thenReturn("RULE_B");

        RuleEngine engine = new RuleEngine(List.of(ruleA, ruleB));

        RiskResult result = engine.assess(tx);

        assertEquals(60, result.getTotalScore());
        assertEquals(2, result.getReasons().size());
        assertEquals(List.of("RULE_A", "RULE_B"), result.getRuleNames());
        assertEquals(TransactionStatus.BLOCKED, result.getStatus());

        // both rules were asked, so the engine did not stop early
        verify(ruleA).evaluate(tx);
        verify(ruleB).evaluate(tx);
    }

    @Test
    void shouldFlagWhenOnlyOneRuleFires() {
        Transaction tx = new Transaction();

        // rule A fires (30 points), rule B says clean
        when(ruleA.evaluate(tx)).thenReturn(RuleResult.suspicious("rule A flagged it"));
        when(ruleA.getWeight()).thenReturn(30);
        when(ruleA.getName()).thenReturn("RULE_A");
        when(ruleB.evaluate(tx)).thenReturn(RuleResult.clean());

        RuleEngine engine = new RuleEngine(List.of(ruleA, ruleB));

        RiskResult result = engine.assess(tx);

        assertEquals(30, result.getTotalScore());
        assertEquals(1, result.getReasons().size());
        assertEquals(List.of("RULE_A"), result.getRuleNames());
        assertEquals(TransactionStatus.FLAGGED, result.getStatus());
    }

    @Test
    void shouldApproveWhenNoRuleFires() {
        Transaction tx = new Transaction();

        when(ruleA.evaluate(tx)).thenReturn(RuleResult.clean());
        when(ruleB.evaluate(tx)).thenReturn(RuleResult.clean());

        RuleEngine engine = new RuleEngine(List.of(ruleA, ruleB));

        RiskResult result = engine.assess(tx);

        assertEquals(0, result.getTotalScore());
        assertEquals(0, result.getReasons().size());
        assertEquals(TransactionStatus.APPROVED, result.getStatus());
    }
}