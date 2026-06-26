package com.vaishnav.fraud_detection.repository;

import com.vaishnav.fraud_detection.model.Transaction;
import com.vaishnav.fraud_detection.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findTop1ByAccountIdOrderByTimestampDesc(String accountId);

    long countByAccountIdAndTimestampAfter(String accountId, LocalDateTime time);

    List<Transaction> findTop10ByAccountIdOrderByTimestampDesc(String accountId);

    long countByStatus(TransactionStatus status);

    @Query("""
            SELECT t.merchant, COUNT(DISTINCT t.accountId)
            FROM Transaction t
            WHERE t.timestamp > :cutoff
            GROUP BY t.merchant
            HAVING COUNT(DISTINCT t.accountId) >= 3
            """)
    List<Object[]> findSuspiciousMerchants(@Param("cutoff") LocalDateTime cutoff);

    @Query("""
            SELECT t.receiverAccountId, COUNT(DISTINCT t.accountId)
            FROM Transaction t
            WHERE t.timestamp > :cutoff
            GROUP BY t.receiverAccountId
            HAVING COUNT(DISTINCT t.accountId) >= 3
            """)
    List<Object[]> findFanInAccounts(@Param("cutoff") LocalDateTime cutoff);

    @Query("""
            SELECT t1.accountId
            FROM Transaction t1
            WHERE t1.merchant IN (
                SELECT t2.merchant
                FROM Transaction t2
                WHERE t2.accountId = :accountId
            )
            AND t1.accountId <> :accountId
            """)
    List<String> findPotentialCircularAccounts(@Param("accountId") String accountId);
}