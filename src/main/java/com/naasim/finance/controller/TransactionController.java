package com.naasim.finance.controller;

import com.naasim.finance.model.TransactionDto;
import com.naasim.finance.model.TransactionRequest;
import com.naasim.finance.repository.TransactionRepository;
import com.naasim.finance.service.TransactionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    /** Single-user for now. Replace with the logged-in user once authentication is added. */
    static final int USER_ID = 1;

    private final TransactionRepository repo;
    private final TransactionService service;

    public TransactionController(TransactionRepository repo, TransactionService service) {
        this.repo = repo;
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 0);
        List<TransactionDto> items = repo.find(USER_ID, from, to, categoryId, type, size, page * size);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("total", repo.count(USER_ID, from, to, categoryId, type));
        out.put("page", page);
        out.put("size", size);
        return out;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionDto create(@RequestBody TransactionRequest r) {
        return service.create(USER_ID, r);
    }

    /** Atomic batch insert: all rows are saved or none are. */
    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<TransactionDto> bulk(@RequestBody List<TransactionRequest> requests) {
        return service.createBulk(USER_ID, requests);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionDto> update(@PathVariable long id, @RequestBody TransactionRequest r) {
        r.validate();
        repo.update(USER_ID, id, r);
        TransactionDto saved = repo.findById(USER_ID, id);
        return saved == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return repo.delete(USER_ID, id) == 0 ? ResponseEntity.notFound().build() : ResponseEntity.noContent().build();
    }
}
