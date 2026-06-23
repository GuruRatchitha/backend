-- Run this manually on the MySQL database used by application.yml.
-- This project uses ddl-auto=update, but Hibernate cannot safely change existing primary keys.
-- Take a backup before running this on shared data.

USE dev_db;

ALTER TABLE users
    ADD COLUMN country_code VARCHAR(255) NOT NULL DEFAULT 'us',
    ADD COLUMN town_name VARCHAR(255) NULL;

ALTER TABLE beneficiary
    DROP PRIMARY KEY,
    ADD COLUMN beneficiary_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST,
    ADD CONSTRAINT uk_beneficiary_user_account_routing UNIQUE (user_id, account_number, routing_number);

ALTER TABLE transactions
    ADD COLUMN transfer_id VARCHAR(35) NULL,
    ADD COLUMN payment_transaction_id VARCHAR(35) NULL,
    ADD COLUMN bank_transaction_id VARCHAR(22) NULL,
    ADD COLUMN beneficiary_account_number VARCHAR(255) NULL,
    ADD COLUMN beneficiary_routing_number VARCHAR(255) NULL;

-- Existing rows need generated placeholder values before NOT NULL and UNIQUE constraints can be applied.
UPDATE transactions
SET
    transfer_id = COALESCE(transfer_id, LEFT(UUID(), 35)),
    payment_transaction_id = COALESCE(payment_transaction_id, LEFT(UUID(), 35)),
    bank_transaction_id = COALESCE(bank_transaction_id, CONCAT(DATE_FORMAT(CURRENT_DATE, '%Y%m%d'), 'MIGRATE', LPAD(transaction_id, 7, '0'))),
    beneficiary_account_number = COALESCE(beneficiary_account_number, ''),
    beneficiary_routing_number = COALESCE(beneficiary_routing_number, '');

ALTER TABLE transactions
    MODIFY transfer_id VARCHAR(35) NOT NULL,
    MODIFY payment_transaction_id VARCHAR(35) NOT NULL,
    MODIFY bank_transaction_id VARCHAR(22) NOT NULL,
    MODIFY beneficiary_account_number VARCHAR(255) NOT NULL,
    MODIFY beneficiary_routing_number VARCHAR(255) NOT NULL,
    ADD CONSTRAINT uk_transactions_transfer_id UNIQUE (transfer_id),
    ADD CONSTRAINT uk_transactions_payment_transaction_id UNIQUE (payment_transaction_id),
    ADD CONSTRAINT uk_transactions_bank_transaction_id UNIQUE (bank_transaction_id);

-- The payment module stores message_id as the generated BizMsgIdr/MsgId string.
-- If you do not need old message_header/pacs008 data, clearing these two tables is the cleanest migration.
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE pacs008;
TRUNCATE TABLE message_header;
SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE message_header
    MODIFY message_id VARCHAR(22) NOT NULL,
    MODIFY business_message_id VARCHAR(22) NOT NULL;

DROP TABLE IF EXISTS pacs008;

CREATE TABLE pacs008 (
    pacs008_id BIGINT NOT NULL AUTO_INCREMENT,
    transaction_id BIGINT NOT NULL,
    message_id VARCHAR(22) NOT NULL,
    transfer_id VARCHAR(35) NOT NULL,
    instruction_id VARCHAR(35) NOT NULL,
    tx_id VARCHAR(35) NOT NULL,
    from_mmb_id VARCHAR(9) NOT NULL,
    to_mmb_id VARCHAR(9) NOT NULL,
    instg_agt_mmb_id VARCHAR(9) NOT NULL,
    instd_agt_mmb_id VARCHAR(9) NOT NULL,
    dbtr_agt_mmb_id VARCHAR(9) NOT NULL,
    cdtr_agt_mmb_id VARCHAR(9) NOT NULL,
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
