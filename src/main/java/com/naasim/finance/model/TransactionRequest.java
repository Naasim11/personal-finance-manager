package com.naasim.finance.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(Integer categoryId, BigDecimal amount, LocalDate date, String note) {
    public void validate() {
        if (categoryId == null) throw new IllegalArgumentException("Choose a category.");
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
        if (date == null) throw new IllegalArgumentException("Enter a date.");
        if (note != null && note.length() > 255) throw new IllegalArgumentException("Note must be 255 characters or fewer.");
    }
}
