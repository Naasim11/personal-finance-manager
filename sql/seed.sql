USE finance_db;

INSERT INTO users (name, email) VALUES ('Naasim', 'naasim@example.com');

INSERT INTO categories (user_id, name, type) VALUES
 (1,'Salary','INCOME'),(1,'Freelance','INCOME'),
 (1,'Rent','EXPENSE'),(1,'Groceries','EXPENSE'),(1,'Transport','EXPENSE'),
 (1,'Dining out','EXPENSE'),(1,'Utilities','EXPENSE'),(1,'Entertainment','EXPENSE');

-- Budgets first, so the trigger can flag over-budget transactions
INSERT INTO budgets (user_id, category_id, month_start, limit_amount) VALUES
 (1,3,'2026-10-01',15000),(1,4,'2026-10-01',6000),(1,5,'2026-10-01',2500),
 (1,6,'2026-10-01',3000),(1,7,'2026-10-01',2500),(1,8,'2026-10-01',2000),
 (1,3,'2026-09-01',15000),(1,4,'2026-09-01',6000),(1,6,'2026-09-01',3000);

INSERT INTO transactions (user_id, category_id, amount, txn_date, note) VALUES
 -- August
 (1,1,60000,'2026-08-01','August salary'),(1,3,15000,'2026-08-02','Rent'),
 (1,4,5200,'2026-08-09','Monthly groceries'),(1,7,2100,'2026-08-14','Electricity + internet'),
 (1,6,2400,'2026-08-21','Dinner with friends'),
 -- September
 (1,1,60000,'2026-09-01','September salary'),(1,2,8000,'2026-09-12','Website project'),
 (1,3,15000,'2026-09-02','Rent'),(1,4,6400,'2026-09-08','Groceries'),
 (1,5,1800,'2026-09-15','Bus pass + fuel'),(1,6,3600,'2026-09-20','Restaurants'),
 (1,7,2300,'2026-09-14','Utilities'),
 -- October
 (1,1,60000,'2026-10-01','October salary'),(1,2,12000,'2026-10-10','Freelance logo work'),
 (1,3,15000,'2026-10-02','Rent'),(1,4,3100,'2026-10-05','Groceries'),
 (1,4,3400,'2026-10-19','Groceries (festival shopping)'),(1,5,1900,'2026-10-07','Fuel'),
 (1,6,1200,'2026-10-11','Lunch out'),(1,7,2200,'2026-10-13','Electricity'),
 (1,8,900,'2026-10-18','Movie night');
