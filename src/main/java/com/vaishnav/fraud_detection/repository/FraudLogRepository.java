package com.vaishnav.fraud_detection.repository;

import com.vaishnav.fraud_detection.model.FraudLog;
import com.vaishnav.fraud_detection.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FraudLogRepository extends JpaRepository<FraudLog, Long> {

    Optional<FraudLog> findByTransaction(Transaction transaction);

}
