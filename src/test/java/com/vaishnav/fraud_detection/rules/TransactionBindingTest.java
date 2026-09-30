package com.vaishnav.fraud_detection.rules;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.dto.TransactionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionBindingTest {

    // Spring Boot's own mapper ignores unknown fields, so we copy that setting
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String SNEAKY_JSON = """
            {
              "id": 1,
              "accountId": "ACC001",
              "amount": 100,
              "currency": "INR",
              "merchant": "Amazon",
              "country": "IN"
            }
            """;

    // Proves the problem: the entity accepts an id sent by the client
    @Test
    void entityAcceptsIdFromClient() throws Exception {
        Transaction tx = mapper.readValue(SNEAKY_JSON, Transaction.class);

        assertEquals(1L, tx.getId());
    }

    // Proves the fix: the DTO has no id field at all, so there is nowhere to put it
    @Test
    void dtoHasNoIdStatusOrTimestampField() {
        assertThrows(NoSuchFieldException.class,
                () -> TransactionRequest.class.getDeclaredField("id"));
        assertThrows(NoSuchFieldException.class,
                () -> TransactionRequest.class.getDeclaredField("status"));
        assertThrows(NoSuchFieldException.class,
                () -> TransactionRequest.class.getDeclaredField("timestamp"));
    }
}