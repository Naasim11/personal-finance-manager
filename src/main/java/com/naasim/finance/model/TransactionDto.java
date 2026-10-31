package com.naasim.finance.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionDto(Long id, int categoryId, String categoryName, String type,
                             BigDecimal amount, LocalDate date, String note, boolean overBudget) {}
