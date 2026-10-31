# Personal Finance Management System

Track income and expenses, set monthly budgets, browse transaction history and view monthly reports.

**Stack:** Java 17 + Spring Boot (JDBC, no ORM) · MySQL 8 · vanilla JavaScript + Chart.js

## Run it

1. Create the database and sample data:
   ```bash
   mysql -u root -p < sql/schema.sql
   mysql -u root -p < sql/seed.sql
   ```
2. Set your MySQL credentials (or edit `application.properties`):
   ```bash
   export DB_USER=root
   export DB_PASSWORD=your_password
   ```
3. Start the app (needs JDK 17+ and Maven):
   ```bash
   mvn spring-boot:run
   ```
4. Open http://localhost:8080

## Database operations demonstrated

| Concept | Where |
|---|---|
| Normalised schema, foreign keys, `CHECK`, `UNIQUE`, `ENUM` | `sql/schema.sql` |
| Composite indexes for history and category queries | `idx_user_date`, `idx_user_cat_date` |
| View | `monthly_summary` (used by reports) |
| Stored procedure | `sp_is_over_budget` |
| Triggers | `trg_txn_flag_insert`, `trg_txn_flag_update` set `over_budget` on a transaction |
| JOIN + GROUP BY + LEFT JOIN (budget vs. actual) | `BudgetRepository.budgetVsActual` |
| Dynamic filtering + pagination with prepared statements | `TransactionRepository.find` |
| Upsert (`ON DUPLICATE KEY UPDATE`) | `BudgetRepository.upsert` |
| `INSERT ... SELECT` | `BudgetRepository.copyMonth` |
| Commit / rollback | `TransactionService.createBulk` (`POST /api/transactions/bulk`) |

## REST API

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/transactions?from&to&type&categoryId&page&size` | Filtered, paged history |
| POST / PUT / DELETE | `/api/transactions[/{id}]` | Create, edit, delete |
| POST | `/api/transactions/bulk` | Atomic batch insert |
| GET / POST | `/api/categories` | List, create |
| GET / POST / DELETE | `/api/budgets` | Budget vs. actual for a month, set, remove |
| POST | `/api/budgets/copy?from=2026-09&to=2026-10` | Copy budgets between months |
| GET | `/api/reports/monthly?year=2026&month=10` | Totals, category breakdown, budgets, 6-month trend |

Try the rollback demo (the second row is invalid, so neither is saved):
```bash
curl -X POST localhost:8080/api/transactions/bulk -H 'Content-Type: application/json' \
  -d '[{"categoryId":4,"amount":100,"date":"2026-10-20"},{"categoryId":4,"amount":-5,"date":"2026-10-20"}]'
```

## Next steps

- Add login (Spring Security + BCrypt) and replace the fixed `USER_ID = 1` in `TransactionController`.
- Add recurring transactions and CSV export.
