package com.naasim.finance.service;

import com.naasim.finance.model.TransactionDto;
import com.naasim.finance.model.TransactionRequest;
import com.naasim.finance.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository repo;

    public TransactionService(TransactionRepository repo) {
        this.repo = repo;
    }

    public TransactionDto create(int userId, TransactionRequest r) {
        r.validate();
        long id = repo.insert(userId, r);
        return repo.findById(userId, id);
    }

    /**
     * Demonstrates commit/rollback: either every row is saved, or none are.
     * If row 3 is invalid, the exception rolls back rows 1 and 2.
     */
    @Transactional
    public List<TransactionDto> createBulk(int userId, List<TransactionRequest> requests) {
        List<TransactionDto> saved = new ArrayList<>();
        for (TransactionRequest r : requests) {
            saved.add(create(userId, r));
        }
        return saved;
    }
}
