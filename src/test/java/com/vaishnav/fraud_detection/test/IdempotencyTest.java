package com.vaishnav.fraud_detection.test;

import com.vaishnav.fraud_detection.dto.TransactionRequest;
import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class IdempotencyTest {

    @Autowired
    private TransactionService transactionService;

    private TransactionRequest makeRequest(String accountId) {
        TransactionRequest request = new TransactionRequest();
        request.setAccountId(accountId);
        request.setAmount(new BigDecimal("100"));
        request.setCurrency("INR");
        request.setMerchant("IdemShop");
        request.setCountry("IN");
        return request;
    }

    @Test
    void sameKeySentTwiceShouldCreateOnlyOneTransaction() {
        TransactionRequest request = makeRequest("IDEM_ACC");

        Transaction first = transactionService.saveTransaction(request, "key-123");
        Transaction second = transactionService.saveTransaction(request, "key-123");

        // the same key means the same request, so both calls must return the same transaction
        assertEquals(first.getId(), second.getId());
    }

    @Test
    void tenParallelRequestsWithSameKeyShouldCreateOnlyOneTransaction() throws Exception {
        int requests = 10;
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<Transaction>> futures = new ArrayList<>();

        for (int i = 0; i < requests; i++) {
            futures.add(pool.submit(() -> {
                startSignal.await();   // all threads wait for the starting gun
                return transactionService.saveTransaction(makeRequest("IDEM_ACC2"), "key-parallel");
            }));
        }

        startSignal.countDown();   // all 10 start at the same moment

        // collect the ids; a Set keeps only different values
        Set<Long> ids = new HashSet<>();
        for (Future<Transaction> future : futures) {
            ids.add(future.get().getId());
        }
        pool.shutdown();

        // all 10 requests must have received the same single transaction
        assertEquals(1, ids.size());
    }
}