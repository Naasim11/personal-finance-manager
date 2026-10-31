package com.naasim.finance.repository;

import com.naasim.finance.model.TransactionDto;
import com.naasim.finance.model.TransactionRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** All SQL uses prepared statements (parameters are bound, never concatenated). */
@Repository
public class TransactionRepository {

    private static final String SELECT = """
        SELECT t.txn_id, t.category_id, c.name AS category_name, c.type, t.amount,
               t.txn_date, t.note, t.over_budget
        FROM transactions t JOIN categories c ON c.category_id = t.category_id
        """;

    private static final RowMapper<TransactionDto> MAPPER = (rs, i) -> new TransactionDto(
            rs.getLong("txn_id"), rs.getInt("category_id"), rs.getString("category_name"),
            rs.getString("type"), rs.getBigDecimal("amount"),
            rs.getObject("txn_date", LocalDate.class), rs.getString("note"),
            rs.getBoolean("over_budget"));

    private final JdbcTemplate jdbc;

    public TransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Builds the WHERE clause shared by the page query and the count query. */
    private static class Filter {
        final StringBuilder where = new StringBuilder(" WHERE t.user_id = ?");
        final List<Object> args = new ArrayList<>();

        Filter(int userId, LocalDate from, LocalDate to, Integer categoryId, String type) {
            args.add(userId);
            if (from != null) { where.append(" AND t.txn_date >= ?"); args.add(from); }
            if (to != null) { where.append(" AND t.txn_date <= ?"); args.add(to); }
            if (categoryId != null) { where.append(" AND t.category_id = ?"); args.add(categoryId); }
            if (type != null && !type.isBlank()) { where.append(" AND c.type = ?"); args.add(type); }
        }
    }

    public List<TransactionDto> find(int userId, LocalDate from, LocalDate to, Integer categoryId,
                                     String type, int limit, int offset) {
        Filter f = new Filter(userId, from, to, categoryId, type);
        String sql = SELECT + f.where + " ORDER BY t.txn_date DESC, t.txn_id DESC LIMIT ? OFFSET ?";
        f.args.add(limit);
        f.args.add(offset);
        return jdbc.query(sql, MAPPER, f.args.toArray());
    }

    public long count(int userId, LocalDate from, LocalDate to, Integer categoryId, String type) {
        Filter f = new Filter(userId, from, to, categoryId, type);
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM transactions t JOIN categories c ON c.category_id = t.category_id" + f.where,
                Long.class, f.args.toArray());
        return n == null ? 0 : n;
    }

    public TransactionDto findById(int userId, long id) {
        List<TransactionDto> rows = jdbc.query(SELECT + " WHERE t.user_id = ? AND t.txn_id = ?", MAPPER, userId, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public long insert(int userId, TransactionRequest r) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO transactions (user_id, category_id, amount, txn_date, note) VALUES (?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, userId);
            ps.setInt(2, r.categoryId());
            ps.setBigDecimal(3, r.amount());
            ps.setObject(4, r.date());
            ps.setString(5, r.note());
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    public int update(int userId, long id, TransactionRequest r) {
        return jdbc.update(
                "UPDATE transactions SET category_id = ?, amount = ?, txn_date = ?, note = ? WHERE txn_id = ? AND user_id = ?",
                r.categoryId(), r.amount(), r.date(), r.note(), id, userId);
    }

    public int delete(int userId, long id) {
        return jdbc.update("DELETE FROM transactions WHERE txn_id = ? AND user_id = ?", id, userId);
    }
}
