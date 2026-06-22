-- Strong fix for: Data truncated for column 'message_id'
-- Run on the same MySQL database configured in application.yml.
-- This deletes old message data and recreates message_header/pacs008 for the payment module.

USE dev_db;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS pacs008;
DROP TABLE IF EXISTS pacs002;
DROP TABLE IF EXISTS adm002;
DROP TABLE IF EXISTS message_header;

CREATE TABLE message_header (
    message_id VARCHAR(22) NOT NULL,
    business_message_id VARCHAR(22) NOT NULL,
    message_type VARCHAR(255) NULL,
    direction VARCHAR(255) NULL,
    message_status VARCHAR(255) NULL,
    created_date DATETIME(6) NULL,
    transaction_id BIGINT NULL,
    PRIMARY KEY (message_id)
);

CREATE TABLE pacs008 (
    pacs008_id BIGINT NOT NULL AUTO_INCREMENT,
    transaction_id BIGINT NOT NULL,
    message_id VARCHAR(22) NOT NULL,
    transfer_id VARCHAR(35) NOT NULL,
    instruction_id VARCHAR(35) NOT NULL,
    end_to_end_id VARCHAR(10) NOT NULL,
    uetr VARCHAR(36) NOT NULL,
    payment_transaction_id VARCHAR(35) NOT NULL,
    bank_transaction_id VARCHAR(22) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    debtor_name VARCHAR(140) NOT NULL,
    debtor_account VARCHAR(255) NOT NULL,
    debtor_town VARCHAR(35) NOT NULL,
    debtor_country VARCHAR(2) NOT NULL,
    creditor_name VARCHAR(140) NOT NULL,
    creditor_account VARCHAR(255) NOT NULL,
    creditor_town VARCHAR(35) NOT NULL,
    creditor_country VARCHAR(2) NOT NULL,
    settlement_date DATE NOT NULL,
    acceptance_datetime DATETIME(6) NOT NULL,
    charge_bearer VARCHAR(4) NOT NULL,
    local_instrument VARCHAR(4) NOT NULL,
    xml_payload TEXT NULL,
    created_date DATETIME(6) NOT NULL,
    PRIMARY KEY (pacs008_id)
);

CREATE TABLE pacs002 (
    pacs002_id BIGINT NOT NULL AUTO_INCREMENT,
    original_message_id VARCHAR(255) NULL,
    transaction_status VARCHAR(255) NULL,
    reason_code VARCHAR(255) NULL,
    xml_payload TEXT NULL,
    transaction_id BIGINT NULL,
    message_id VARCHAR(22) NULL,
    PRIMARY KEY (pacs002_id)
);

CREATE TABLE adm002 (
    adm002_id BIGINT NOT NULL AUTO_INCREMENT,
    original_message_id VARCHAR(255) NULL,
    acknowledgement_code VARCHAR(255) NULL,
    xml_payload TEXT NULL,
    transaction_id BIGINT NULL,
    message_id VARCHAR(22) NULL,
    PRIMARY KEY (adm002_id)
);

SET FOREIGN_KEY_CHECKS = 1;
