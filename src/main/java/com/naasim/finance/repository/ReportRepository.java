package com.naasim.finance.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads from the monthly_summary view defined in sql/schema.sql. */
@Repository
public class ReportRepository {
    private final JdbcTemplate jdbc;

    public ReportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> byCategory(int userId, LocalDate monthStart) {
        return jdbc.query("""
            SELECT category_name, type, total, txn_count FROM monthly_summary
            WHERE user_id = ? AND month_start = ? ORDER BY total DESC
            """, (rs, i) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("category", rs.getString("category_name"));
            m.put("type", rs.getString("type"));
            m.put("total", rs.getBigDecimal("total"));
            m.put("count", rs.getInt("txn_count"));
            return m;
        }, userId, monthStart);
    }

    public List<Map<String, Object>> trend(int userId, LocalDate fromMonth, LocalDate toMonth) {
        return jdbc.query("""
            SELECT month_start, type, SUM(total) AS total FROM monthly_summary
            WHERE user_id = ? AND month_start BETWEEN ? AND ?
            GROUP BY month_start, type ORDER BY month_start
            """, (rs, i) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", rs.getObject("month_start", LocalDate.class));
            m.put("type", rs.getString("type"));
            m.put("total", rs.getBigDecimal("total"));
            return m;
        }, userId, fromMonth, toMonth);
    }
}
