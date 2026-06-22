package com.vaishnav.fraud_detection.rules;

import com.vaishnav.fraud_detection.model.Transaction;

public interface FraudRule  {
    RuleResult evaluate(Transaction tx);
}
