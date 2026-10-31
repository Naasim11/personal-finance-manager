package com.naasim.finance.controller;

import com.naasim.finance.model.CategoryDto;
import com.naasim.finance.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryRepository repo;

    public CategoryController(CategoryRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<CategoryDto> list() {
        return repo.findAll(TransactionController.USER_ID);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@RequestBody CategoryDto c) {
        if (c.name() == null || c.name().isBlank() || c.name().length() > 60)
            throw new IllegalArgumentException("Category name must be 1 to 60 characters.");
        if (!"INCOME".equals(c.type()) && !"EXPENSE".equals(c.type()))
            throw new IllegalArgumentException("Type must be INCOME or EXPENSE.");
        repo.insert(TransactionController.USER_ID, c.name().trim(), c.type());
    }
}
