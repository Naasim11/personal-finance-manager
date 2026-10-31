package com.naasim.finance.controller;

import com.naasim.finance.model.BudgetRequest;
import com.naasim.finance.repository.BudgetRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    private static final int USER = TransactionController.USER_ID;
    private final BudgetRepository repo;

    public BudgetController(BudgetRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam int year, @RequestParam int month) {
        return repo.budgetVsActual(USER, LocalDate.of(year, month, 1));
    }

    @PostMapping
    public void set(@RequestBody BudgetRequest r) {
        if (r.categoryId() == null || r.year() == null || r.month() == null)
            throw new IllegalArgumentException("Choose a category and month.");
        if (r.limit() == null || r.limit().signum() <= 0)
            throw new IllegalArgumentException("Budget must be greater than zero.");
        repo.upsert(USER, r.categoryId(), LocalDate.of(r.year(), r.month(), 1), r.limit());
    }

    /** Copy every budget from one month to another, e.g. /api/budgets/copy?from=2026-09&to=2026-10 */
    @PostMapping("/copy")
    public Map<String, Integer> copy(@RequestParam String from, @RequestParam String to) {
        int n = repo.copyMonth(USER, YearMonth.parse(from).atDay(1), YearMonth.parse(to).atDay(1));
        return Map.of("copied", n);
    }

    @DeleteMapping
    public void delete(@RequestParam int categoryId, @RequestParam int year, @RequestParam int month) {
        repo.delete(USER, categoryId, LocalDate.of(year, month, 1));
    }
}
