package com.madhumitha.transactionservice.repository;

import com.madhumitha.transactionservice.model.Transaction;
import com.madhumitha.transactionservice.model.enums.RiskLevel;
import com.madhumitha.transactionservice.model.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findBySenderId(String senderId);

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findByRiskLevel(RiskLevel riskLevel);

    List<Transaction> findBySenderIdOrderByCreatedAtDesc(String senderId);

    long countByStatus(TransactionStatus status);

    Page<Transaction> findAll(Pageable pageable);

    @Query("""
            SELECT t.riskLevel, CAST(t.createdAt AS LocalDate), COUNT(t), SUM(t.amount)
            FROM Transaction t
            WHERE t.createdAt BETWEEN :from AND :to
            GROUP BY t.riskLevel, CAST(t.createdAt AS LocalDate)
            ORDER BY CAST(t.createdAt AS LocalDate) DESC, t.riskLevel
            """)
    List<Object[]> aggregateByRiskLevelAndDate(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
