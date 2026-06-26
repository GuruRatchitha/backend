CREATE TABLE IF NOT EXISTS customers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(10) NOT NULL,
    aadhar_number VARCHAR(12) NOT NULL,
    pan_card_number VARCHAR(10) NOT NULL,
    address VARCHAR(500) NOT NULL,
    town_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_customers_email (email),
    UNIQUE KEY uk_customers_aadhar_number (aadhar_number),
    UNIQUE KEY uk_customers_pan_card_number (pan_card_number)
);
