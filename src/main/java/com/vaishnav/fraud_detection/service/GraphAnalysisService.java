package com.vaishnav.fraud_detection.service;

import com.vaishnav.fraud_detection.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GraphAnalysisService {

    private final TransactionRepository transactionRepository;

    public List<String> getSuspiciousMerchants() {

        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);

        return transactionRepository.findSuspiciousMerchants(cutoff)
                .stream()
                .map(result -> (String) result[0])
                .toList();
    }

    public List<String> getFanInAccounts() {

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);

        return transactionRepository.findFanInAccounts(cutoff)
                .stream()
                .map(result -> (String) result[0])
                .toList();
    }

    public boolean hasCircularTransaction(String accountId) {

        return !transactionRepository
                .findPotentialCircularAccounts(accountId)
                .isEmpty();
    }
}
