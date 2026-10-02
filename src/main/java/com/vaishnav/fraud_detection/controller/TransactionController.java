package com.vaishnav.fraud_detection.controller;

import com.vaishnav.fraud_detection.model.FraudLog;
import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.dto.TransactionRequest;
import com.vaishnav.fraud_detection.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public Transaction createTransaction(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransactionRequest request) {
        return transactionService.saveTransaction(request, idempotencyKey);
    }

    @GetMapping("/{id}/fraud-report")
    public ResponseEntity<FraudLog> getFraudReport(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getFraudReport(id));
    }
}