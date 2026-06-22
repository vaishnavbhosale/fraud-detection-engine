package com.vaishnav.fraud_detection.repository;

import com.vaishnav.fraud_detection.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findTop1ByAccountIdOrderByTimestampDesc(String accountId);

    // New method for the Velocity Rule
    long countByAccountIdAndTimestampAfter(String accountId, LocalDateTime time);

}