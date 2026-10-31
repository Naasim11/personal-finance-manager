package com.naasim.finance.model;

import java.math.BigDecimal;

public record BudgetRequest(Integer categoryId, Integer year, Integer month, BigDecimal limit) {}
