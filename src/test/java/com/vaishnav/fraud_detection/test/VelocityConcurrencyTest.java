package com.vaishnav.fraud_detection.test;

import com.vaishnav.fraud_detection.dto.TransactionRequest;
import com.vaishnav.fraud_detection.model.AIFraudReport;
import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.model.TransactionStatus;
import com.vaishnav.fraud_detection.service.AIAnalysisService;
import com.vaishnav.fraud_detection.service.AlertService;
import com.vaishnav.fraud_detection.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class VelocityConcurrencyTest {

    @Autowired
    private TransactionService transactionService;

    // fake AI and fake email, so the test does not call the internet
    @MockitoBean
    private AIAnalysisService aiAnalysisService;

    @MockitoBean
    private AlertService alertService;

    @Test
    void tenParallelRequestsShouldNotBypassVelocityLimit() throws Exception {
        when(aiAnalysisService.analyze(any(), any(), any()))
                .thenReturn(new AIFraudReport(3, "NONE", "test", "REVIEW"));

        int requests = 10;
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<Transaction>> futures = new ArrayList<>();

        for (int i = 0; i < requests; i++) {
            futures.add(pool.submit(() -> {
                startSignal.await();   // every thread waits here until the signal

                TransactionRequest request = new TransactionRequest();
                request.setAccountId("RACE_ACC");
                request.setAmount(new BigDecimal("100"));
                request.setCurrency("INR");
                request.setMerchant("RaceShop");
                request.setCountry("IN");

                return transactionService.saveTransaction(request);
            }));
        }

        startSignal.countDown();   // all 10 threads start at the same moment

        int approved = 0;
        for (Future<Transaction> future : futures) {
            Transaction tx = future.get();
            if (tx.getStatus() == TransactionStatus.APPROVED) {
                approved++;
            }
        }
        pool.shutdown();

        // the limit is 5, so exactly 5 should be approved and the other 5 flagged
        assertEquals(5, approved);
    }
}
