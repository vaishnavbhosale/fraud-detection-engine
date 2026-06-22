package com.vaishnav.fraud_detection.rules;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RuleResult {

    private boolean suspicious;

    private String reason;

    public static RuleResult suspicious(String reason) {
        return new RuleResult(true, reason);
    }
    public static RuleResult clean() {
        return new RuleResult(false, "Transaction looks clean");
    }
}
