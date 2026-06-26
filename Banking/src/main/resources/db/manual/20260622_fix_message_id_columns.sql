-- Fix for: Data truncated for column 'message_id'
-- Run this on the same MySQL database configured in application.yml.
-- This clears old generated message data because message_id changed from numeric to Fedwire BizMsgIdr format.

USE dev_db;

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS business_message_sequence (
    sequence_date DATE NOT NULL,
    sequence_value INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (sequence_date)
);

TRUNCATE TABLE pacs008;
TRUNCATE TABLE pacs002;
TRUNCATE TABLE adm002;
TRUNCATE TABLE message_header;

ALTER TABLE message_header
    MODIFY COLUMN message_id VARCHAR(22) NOT NULL,
    MODIFY COLUMN business_message_id VARCHAR(22) NOT NULL;

-- Your existing database may have either messageId or message_id from earlier entity versions.
ALTER TABLE pacs002
    MODIFY COLUMN message_id VARCHAR(22) NULL;

ALTER TABLE adm002
    MODIFY COLUMN message_id VARCHAR(22) NULL;

DROP TABLE IF EXISTS pacs008;

CREATE TABLE pacs008 (
    pacs008_id BIGINT NOT NULL AUTO_INCREMENT,
    transaction_id BIGINT NOT NULL,
    message_id VARCHAR(22) NOT NULL,
    transfer_id VARCHAR(35) NOT NULL,
    instruction_id VARCHAR(35) NOT NULL,
    tx_id VARCHAR(35) NOT NULL,
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
    sqs_published_at DATETIME(6) NULL,
    sqs_message_id VARCHAR(128) NULL,
    created_date DATETIME(6) NOT NULL,
    PRIMARY KEY (pacs008_id),
    CONSTRAINT uk_pacs008_transaction_id UNIQUE (transaction_id)
);

SET FOREIGN_KEY_CHECKS = 1;
