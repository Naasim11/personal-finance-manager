package com.naasim.finance.controller;

import com.naasim.finance.repository.BudgetRepository;
import com.naasim.finance.repository.ReportRepository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private static final int USER = TransactionController.USER_ID;
    private final ReportRepository reports;
    private final BudgetRepository budgets;

    public ReportController(ReportRepository reports, BudgetRepository budgets) {
        this.reports = reports;
        this.budgets = budgets;
    }

    @GetMapping("/monthly")
    public Map<String, Object> monthly(@RequestParam int year, @RequestParam int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        List<Map<String, Object>> byCategory = reports.byCategory(USER, start);

        BigDecimal income = BigDecimal.ZERO, expense = BigDecimal.ZERO;
        for (Map<String, Object> row : byCategory) {
            BigDecimal t = (BigDecimal) row.get("total");
            if ("INCOME".equals(row.get("type"))) income = income.add(t);
            else expense = expense.add(t);
        }

        // Six-month trend ending at the selected month; months with no data show zero.
        LocalDate first = start.minusMonths(5);
        Map<LocalDate, BigDecimal[]> buckets = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) buckets.put(first.plusMonths(i), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        for (Map<String, Object> row : reports.trend(USER, first, start)) {
            BigDecimal[] b = buckets.get((LocalDate) row.get("month"));
            if (b != null) b["INCOME".equals(row.get("type")) ? 0 : 1] = (BigDecimal) row.get("total");
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        buckets.forEach((m, v) -> {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("month", m.getYear() + "-" + String.format("%02d", m.getMonthValue()));
            o.put("income", v[0]);
            o.put("expense", v[1]);
            trend.add(o);
        });

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("year", year);
        out.put("month", month);
        out.put("income", income);
        out.put("expense", expense);
        out.put("net", income.subtract(expense));
        out.put("byCategory", byCategory);
        out.put("budgets", budgets.budgetVsActual(USER, start));
        out.put("trend", trend);
        return out;
    }
}
