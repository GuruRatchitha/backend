package com.bank.fedwire.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureUserColumns();
        ensureAccountColumns();
        ensureBeneficiaryIdColumn();
        ensureTransactionColumns();
        ensurePaymentMessageTables();
    }

    private void ensureUserColumns() {
        addColumnIfMissing("users", "country_code", "ALTER TABLE users ADD COLUMN country_code VARCHAR(255) NOT NULL DEFAULT 'us'");
        addColumnIfMissing("users", "town_name", "ALTER TABLE users ADD COLUMN town_name VARCHAR(255) NULL");
    }

    private void ensureAccountColumns() {
        dropForeignKeysForColumn("account", "customer_id");
        dropColumnIfExists("account", "customer_id", "ALTER TABLE account DROP COLUMN customer_id");
        dropColumnIfExists("account", "initial_balance", "ALTER TABLE account DROP COLUMN initial_balance");
    }

    private void ensureBeneficiaryIdColumn() {
        addColumnIfMissing("beneficiary", "beneficiary_id",
                "ALTER TABLE beneficiary ADD COLUMN beneficiary_id BIGINT NOT NULL AUTO_INCREMENT UNIQUE FIRST");
        addColumnIfMissing("beneficiary", "bank_name",
                "ALTER TABLE beneficiary ADD COLUMN bank_name VARCHAR(255) NULL");
    }

    private void ensureTransactionColumns() {
        addColumnIfMissing("transactions", "transfer_id", "ALTER TABLE transactions ADD COLUMN transfer_id VARCHAR(35) NULL");
        addColumnIfMissing("transactions", "payment_transaction_id", "ALTER TABLE transactions ADD COLUMN payment_transaction_id VARCHAR(35) NULL");
        addColumnIfMissing("transactions", "bank_transaction_id", "ALTER TABLE transactions ADD COLUMN bank_transaction_id VARCHAR(22) NULL");
        addColumnIfMissing("transactions", "beneficiary_account_number", "ALTER TABLE transactions ADD COLUMN beneficiary_account_number VARCHAR(255) NULL");
        addColumnIfMissing("transactions", "beneficiary_routing_number", "ALTER TABLE transactions ADD COLUMN beneficiary_routing_number VARCHAR(255) NULL");
        addColumnIfMissing("transactions", "pending_payment_key", "ALTER TABLE transactions ADD COLUMN pending_payment_key VARCHAR(64) NULL");
        addUniqueIndexIfMissing("transactions", "uk_transactions_pending_payment_key",
                "ALTER TABLE transactions ADD CONSTRAINT uk_transactions_pending_payment_key UNIQUE (pending_payment_key)");
    }

    private void ensurePaymentMessageTables() {
        if (!tableExists("message_header") || !tableExists("pacs008") || !isMessageIdVarchar22()) {
            recreatePaymentMessageTables();
            return;
        }

        ensurePacs008Columns();
    }

    private void ensurePacs008Columns() {
        addColumnIfMissing("pacs008", "tx_id", "ALTER TABLE pacs008 ADD COLUMN tx_id VARCHAR(35) NOT NULL DEFAULT '' AFTER instruction_id");
        dropColumnIfExists("pacs008", "from_mmb_id", "ALTER TABLE pacs008 DROP COLUMN from_mmb_id");
        dropColumnIfExists("pacs008", "to_mmb_id", "ALTER TABLE pacs008 DROP COLUMN to_mmb_id");
        dropColumnIfExists("pacs008", "instg_agt_mmb_id", "ALTER TABLE pacs008 DROP COLUMN instg_agt_mmb_id");
        dropColumnIfExists("pacs008", "instd_agt_mmb_id", "ALTER TABLE pacs008 DROP COLUMN instd_agt_mmb_id");
        dropColumnIfExists("pacs008", "dbtr_agt_mmb_id", "ALTER TABLE pacs008 DROP COLUMN dbtr_agt_mmb_id");
        dropColumnIfExists("pacs008", "cdtr_agt_mmb_id", "ALTER TABLE pacs008 DROP COLUMN cdtr_agt_mmb_id");
        addUniqueIndexIfMissing("pacs008", "uk_pacs008_transaction_id",
                "ALTER TABLE pacs008 ADD CONSTRAINT uk_pacs008_transaction_id UNIQUE (transaction_id)");
    }

    private void recreatePaymentMessageTables() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("DROP TABLE IF EXISTS pacs008");
        jdbcTemplate.execute("DROP TABLE IF EXISTS pacs002");
        jdbcTemplate.execute("DROP TABLE IF EXISTS adm002");
        jdbcTemplate.execute("DROP TABLE IF EXISTS message_header");

        jdbcTemplate.execute("""
                CREATE TABLE message_header (
                    message_id VARCHAR(22) NOT NULL,
                    business_message_id VARCHAR(22) NOT NULL,
                    message_type VARCHAR(255) NULL,
                    direction VARCHAR(255) NULL,
                    message_status VARCHAR(255) NULL,
                    created_date DATETIME(6) NULL,
                    transaction_id BIGINT NULL,
                    PRIMARY KEY (message_id)
                )
                """);

        jdbcTemplate.execute("""
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
                    created_date DATETIME(6) NOT NULL,
                    PRIMARY KEY (pacs008_id),
                    CONSTRAINT uk_pacs008_transaction_id UNIQUE (transaction_id)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE pacs002 (
                    pacs002_id BIGINT NOT NULL AUTO_INCREMENT,
                    original_message_id VARCHAR(255) NULL,
                    transaction_status VARCHAR(255) NULL,
                    reason_code VARCHAR(255) NULL,
                    xml_payload TEXT NULL,
                    transaction_id BIGINT NULL,
                    message_id VARCHAR(22) NULL,
                    PRIMARY KEY (pacs002_id)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE adm002 (
                    adm002_id BIGINT NOT NULL AUTO_INCREMENT,
                    original_message_id VARCHAR(255) NULL,
                    acknowledgement_code VARCHAR(255) NULL,
                    xml_payload TEXT NULL,
                    transaction_id BIGINT NULL,
                    message_id VARCHAR(22) NULL,
                    PRIMARY KEY (adm002_id)
                )
                """);

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    private void addColumnIfMissing(String tableName, String columnName, String ddl) {
        if (!columnExists(tableName, columnName)) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void dropColumnIfExists(String tableName, String columnName, String ddl) {
        if (columnExists(tableName, columnName)) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void dropForeignKeysForColumn(String tableName, String columnName) {
        if (!columnExists(tableName, columnName)) {
            return;
        }

        jdbcTemplate.queryForList("""
                SELECT CONSTRAINT_NAME
                FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                  AND REFERENCED_TABLE_NAME IS NOT NULL
                """, String.class, tableName, columnName).forEach(constraintName ->
                jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP FOREIGN KEY `" + constraintName + "`"));
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private void addUniqueIndexIfMissing(String tableName, String indexName, String ddl) {
        if (!indexExists(tableName, indexName)) {
            jdbcTemplate.execute(ddl);
        }
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND INDEX_NAME = ?
                """, Integer.class, tableName, indexName);
        return count != null && count > 0;
    }

    private boolean isMessageIdVarchar22() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'message_header'
                  AND COLUMN_NAME = 'message_id'
                  AND DATA_TYPE = 'varchar'
                  AND CHARACTER_MAXIMUM_LENGTH = 22
                """, Integer.class);
        return count != null && count > 0;
    }
}
