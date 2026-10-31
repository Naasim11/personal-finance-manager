-- Personal Finance Management System: schema, view, procedure and triggers.
-- Run with: mysql -u root -p < sql/schema.sql
DROP DATABASE IF EXISTS finance_db;
CREATE DATABASE finance_db CHARACTER SET utf8mb4;
USE finance_db;

CREATE TABLE users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  name    VARCHAR(100) NOT NULL,
  email   VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE categories (
  category_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id     INT NOT NULL,
  name        VARCHAR(60) NOT NULL,
  type        ENUM('INCOME','EXPENSE') NOT NULL,
  UNIQUE (user_id, name, type),
  FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE transactions (
  txn_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     INT NOT NULL,
  category_id INT NOT NULL,
  amount      DECIMAL(12,2) NOT NULL CHECK (amount > 0),
  txn_date    DATE NOT NULL,
  note        VARCHAR(255),
  over_budget BOOLEAN NOT NULL DEFAULT FALSE,   -- set by trigger
  INDEX idx_user_date (user_id, txn_date),
  INDEX idx_user_cat_date (user_id, category_id, txn_date),
  FOREIGN KEY (user_id) REFERENCES users(user_id),
  FOREIGN KEY (category_id) REFERENCES categories(category_id)
);

CREATE TABLE budgets (
  budget_id    INT AUTO_INCREMENT PRIMARY KEY,
  user_id      INT NOT NULL,
  category_id  INT NOT NULL,
  month_start  DATE NOT NULL,                   -- always the 1st of the month
  limit_amount DECIMAL(12,2) NOT NULL CHECK (limit_amount > 0),
  UNIQUE (user_id, category_id, month_start),
  FOREIGN KEY (user_id) REFERENCES users(user_id),
  FOREIGN KEY (category_id) REFERENCES categories(category_id)
);

-- View: per-month totals by category (used by the monthly report)
CREATE VIEW monthly_summary AS
SELECT t.user_id,
       DATE_SUB(t.txn_date, INTERVAL DAYOFMONTH(t.txn_date) - 1 DAY) AS month_start,
       c.category_id, c.name AS category_name, c.type,
       SUM(t.amount) AS total,
       COUNT(*)      AS txn_count
FROM transactions t
JOIN categories c ON c.category_id = t.category_id
GROUP BY t.user_id, month_start, c.category_id, c.name, c.type;

DELIMITER //

-- Stored procedure: would this amount push the category past its monthly budget?
CREATE PROCEDURE sp_is_over_budget(
  IN  p_user INT, IN p_cat INT, IN p_date DATE,
  IN  p_amount DECIMAL(12,2), IN p_exclude BIGINT,
  OUT p_flag BOOLEAN)
BEGIN
  DECLARE v_limit DECIMAL(12,2) DEFAULT NULL;
  DECLARE v_spent DECIMAL(12,2) DEFAULT 0;
  DECLARE v_start DATE;
  SET p_flag = FALSE;
  SET v_start = DATE_SUB(p_date, INTERVAL DAYOFMONTH(p_date) - 1 DAY);

  SELECT limit_amount INTO v_limit FROM budgets
   WHERE user_id = p_user AND category_id = p_cat AND month_start = v_start;

  IF v_limit IS NOT NULL THEN
    SELECT COALESCE(SUM(amount), 0) INTO v_spent FROM transactions
     WHERE user_id = p_user AND category_id = p_cat
       AND txn_date >= v_start AND txn_date < v_start + INTERVAL 1 MONTH
       AND txn_id <> p_exclude;
    IF v_spent + p_amount > v_limit THEN
      SET p_flag = TRUE;
    END IF;
  END IF;
END//

-- Triggers: flag transactions that exceed the budget
CREATE TRIGGER trg_txn_flag_insert BEFORE INSERT ON transactions
FOR EACH ROW
BEGIN
  CALL sp_is_over_budget(NEW.user_id, NEW.category_id, NEW.txn_date, NEW.amount, 0, @flag);
  SET NEW.over_budget = @flag;
END//

CREATE TRIGGER trg_txn_flag_update BEFORE UPDATE ON transactions
FOR EACH ROW
BEGIN
  CALL sp_is_over_budget(NEW.user_id, NEW.category_id, NEW.txn_date, NEW.amount, OLD.txn_id, @flag);
  SET NEW.over_budget = @flag;
END//

DELIMITER ;
