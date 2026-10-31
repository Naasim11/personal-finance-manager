package com.naasim.finance.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class BudgetRepository {
    private final JdbcTemplate jdbc;

    public BudgetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Budget vs. actual: LEFT JOIN so categories with no spending still show 0. */
    public List<Map<String, Object>> budgetVsActual(int userId, LocalDate monthStart) {
        return jdbc.query("""
            SELECT c.category_id, c.name, b.limit_amount, COALESCE(SUM(t.amount), 0) AS spent
            FROM budgets b
            JOIN categories c ON c.category_id = b.category_id
            LEFT JOIN transactions t ON t.category_id = b.category_id
                 AND t.user_id = b.user_id
                 AND t.txn_date >= b.month_start
                 AND t.txn_date <  b.month_start + INTERVAL 1 MONTH
            WHERE b.user_id = ? AND b.month_start = ?
            GROUP BY c.category_id, c.name, b.limit_amount
            ORDER BY c.name
            """, (rs, i) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("categoryId", rs.getInt("category_id"));
            m.put("category", rs.getString("name"));
            m.put("limit", rs.getBigDecimal("limit_amount"));
            m.put("spent", rs.getBigDecimal("spent"));
            return m;
        }, userId, monthStart);
    }

    /** Insert or update in one atomic statement (relies on UNIQUE(user, category, month)). */
    public void upsert(int userId, int categoryId, LocalDate monthStart, BigDecimal limit) {
        jdbc.update("""
            INSERT INTO budgets (user_id, category_id, month_start, limit_amount) VALUES (?,?,?,?)
            ON DUPLICATE KEY UPDATE limit_amount = VALUES(limit_amount)
            """, userId, categoryId, monthStart, limit);
    }

    /** Copies budgets into the target month, leaving any that already exist untouched. */
    public int copyMonth(int userId, LocalDate from, LocalDate to) {
        return jdbc.update("""
            INSERT IGNORE INTO budgets (user_id, category_id, month_start, limit_amount)
            SELECT user_id, category_id, ?, limit_amount FROM budgets WHERE user_id = ? AND month_start = ?
            """, to, userId, from);
    }

    public int delete(int userId, int categoryId, LocalDate monthStart) {
        return jdbc.update("DELETE FROM budgets WHERE user_id = ? AND category_id = ? AND month_start = ?",
                userId, categoryId, monthStart);
    }
}
