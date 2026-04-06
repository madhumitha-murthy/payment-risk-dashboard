package com.madhumitha.transactionservice.repository;

import com.madhumitha.transactionservice.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    List<Transaction> findBySenderId(String senderId);
    List<Transaction> findByStatus(String status);
    List<Transaction> findByRiskLevel(String riskLevel);
    List<Transaction> findBySenderIdOrderByCreatedAtDesc(String senderId);
    long countByStatus(String status);
}
