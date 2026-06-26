CREATE TABLE customers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(10) NOT NULL,
    aadhar_number VARCHAR(12) NOT NULL,
    pan_card_number VARCHAR(10) NOT NULL,
    address VARCHAR(255) NOT NULL,
    town_name VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_customers_email UNIQUE (email),
    CONSTRAINT uk_customers_aadhar_number UNIQUE (aadhar_number),
    CONSTRAINT uk_customers_pan_card_number UNIQUE (pan_card_number)
);

ALTER TABLE account
    MODIFY account_number VARCHAR(255) NULL,
    ADD CONSTRAINT uk_account_account_number UNIQUE (account_number);
