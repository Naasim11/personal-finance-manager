package com.naasim.finance.repository;

import com.naasim.finance.model.CategoryDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CategoryRepository {
    private final JdbcTemplate jdbc;

    public CategoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CategoryDto> findAll(int userId) {
        return jdbc.query("SELECT category_id, name, type FROM categories WHERE user_id = ? ORDER BY type, name",
                (rs, i) -> new CategoryDto(rs.getInt(1), rs.getString(2), rs.getString(3)), userId);
    }

    public void insert(int userId, String name, String type) {
        jdbc.update("INSERT INTO categories (user_id, name, type) VALUES (?,?,?)", userId, name, type);
    }
}
