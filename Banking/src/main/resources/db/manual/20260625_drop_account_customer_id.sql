ALTER TABLE account
    DROP FOREIGN KEY fk_account_customer;

ALTER TABLE account
    DROP COLUMN customer_id;

ALTER TABLE account
    DROP COLUMN initial_balance;
