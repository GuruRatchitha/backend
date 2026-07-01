package com.bank.fedwire.config;

import com.bank.fedwire.util.RoutingNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaymentSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final RoutingNumberGenerator routingNumberGenerator;

    @Override
    public void run(ApplicationArguments args) {
        ensureUserColumns();
        ensureAadharColumnLengths();
        ensureAccountColumns();
        ensureBeneficiaryIdColumn();
        ensureTransactionColumns();
        ensureBusinessMessageSequenceTable();
        ensureSettlementTransactionTable();
        ensurePaymentMessageTables();
    }

    private void ensureUserColumns() {
        addColumnIfMissing("users", "country_code", "ALTER TABLE users ADD COLUMN country_code VARCHAR(255) NOT NULL DEFAULT 'us'");
        addColumnIfMissing("users", "town_name", "ALTER TABLE users ADD COLUMN town_name VARCHAR(255) NULL");
    }

    private void ensureAadharColumnLengths() {
        expandColumnIfShorterThan("users", "aadhar_number", 12,
                "ALTER TABLE users MODIFY COLUMN aadhar_number VARCHAR(12) NOT NULL");
        expandColumnIfShorterThan("customers", "aadhar_number", 12,
                "ALTER TABLE customers MODIFY COLUMN aadhar_number VARCHAR(12) NOT NULL");
    }

    private void ensureAccountColumns() {
        dropForeignKeysForColumn("account", "customer_id");
        dropColumnIfExists("account", "customer_id", "ALTER TABLE account DROP COLUMN customer_id");
        dropColumnIfExists("account", "initial_balance", "ALTER TABLE account DROP COLUMN initial_balance");
        addColumnIfMissing("account", "account_name", "ALTER TABLE account ADD COLUMN account_name VARCHAR(255) NULL AFTER account_number");
        addColumnIfMissing("account", "routing_number", "ALTER TABLE account ADD COLUMN routing_number VARCHAR(9) NULL AFTER iban");
        ensureAccountRoutingNumbers();
        jdbcTemplate.execute("ALTER TABLE account MODIFY COLUMN routing_number VARCHAR(9) NOT NULL");
        addUniqueIndexIfMissing("account", "uk_account_routing_number",
                "ALTER TABLE account ADD CONSTRAINT uk_account_routing_number UNIQUE (routing_number)");
        addColumnIfMissing("account", "updated_date", "ALTER TABLE account ADD COLUMN updated_date DATETIME(6) NULL AFTER balance");
    }

    private void ensureAccountRoutingNumbers() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT account_id, routing_number
                FROM account
                ORDER BY account_id
                """);
        Set<String> usedRoutingNumbers = new HashSet<>();
        for (Map<String, Object> row : rows) {
            Object routingNumberValue = row.get("routing_number");
            String routingNumber = routingNumberValue != null ? routingNumberValue.toString().trim() : null;
            if (routingNumber != null && routingNumber.matches("\\d{9}") && usedRoutingNumbers.add(routingNumber)) {
                continue;
            }

            String generatedRoutingNumber = generateUniqueRoutingNumber(usedRoutingNumbers);
            jdbcTemplate.update(
                    "UPDATE account SET routing_number = ? WHERE account_id = ?",
                    generatedRoutingNumber,
                    row.get("account_id"));
        }
    }

    private String generateUniqueRoutingNumber(Set<String> usedRoutingNumbers) {
        for (int attempt = 0; attempt < 100; attempt++) {
            String routingNumber = routingNumberGenerator.generate();
            if (usedRoutingNumbers.add(routingNumber)) {
                return routingNumber;
            }
        }
        throw new IllegalStateException("Unable to generate a unique routing number for existing accounts.");
    }

    private void ensureBeneficiaryIdColumn() {
        if (tableExists("beneficiary") && !columnExists("beneficiary", "beneficiary_id") && columnExists("beneficiary", "id")) {
            jdbcTemplate.execute("ALTER TABLE beneficiary CHANGE COLUMN id beneficiary_id BIGINT NOT NULL AUTO_INCREMENT");
        }
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
        // TEMPORARY FOR PAYAPT ADM.002 TESTING
        alterColumnIfExists("transactions", "amount",
                "ALTER TABLE transactions MODIFY COLUMN amount DECIMAL(65, 2) NOT NULL");
        alterColumnIfExists("transactions", "beneficiary_account_number",
                "ALTER TABLE transactions MODIFY COLUMN beneficiary_account_number VARCHAR(1024) NOT NULL");
        alterColumnIfExists("transactions", "beneficiary_routing_number",
                "ALTER TABLE transactions MODIFY COLUMN beneficiary_routing_number VARCHAR(1024) NOT NULL");
        // END TEMPORARY
        addUniqueIndexIfMissing("transactions", "uk_transactions_pending_payment_key",
                "ALTER TABLE transactions ADD CONSTRAINT uk_transactions_pending_payment_key UNIQUE (pending_payment_key)");
        addIndexIfMissing("transactions", "idx_transactions_queue_date",
                "ALTER TABLE transactions ADD INDEX idx_transactions_queue_date (transaction_date_time DESC)");
        addIndexIfMissing("transactions", "idx_transactions_account_number",
                "ALTER TABLE transactions ADD INDEX idx_transactions_account_number (account_number)");
    }

    private void ensurePaymentMessageTables() {
        if (!tableExists("message_header") || !tableExists("pacs008") || !isMessageIdVarchar22()) {
            recreatePaymentMessageTables();
            return;
        }

        ensurePacs008Columns();
        ensurePacs002Columns();
        ensureAdmi002Columns();
    }

    private void ensurePacs008Columns() {
        addColumnIfMissing("pacs008", "tx_id", "ALTER TABLE pacs008 ADD COLUMN tx_id VARCHAR(35) NOT NULL DEFAULT '' AFTER instruction_id");
        addColumnIfMissing("pacs008", "sqs_published_at", "ALTER TABLE pacs008 ADD COLUMN sqs_published_at DATETIME(6) NULL AFTER xml_payload");
        addColumnIfMissing("pacs008", "sqs_message_id", "ALTER TABLE pacs008 ADD COLUMN sqs_message_id VARCHAR(128) NULL AFTER sqs_published_at");
        dropColumnIfExists("pacs008", "from_mmb_id", "ALTER TABLE pacs008 DROP COLUMN from_mmb_id");
        dropColumnIfExists("pacs008", "to_mmb_id", "ALTER TABLE pacs008 DROP COLUMN to_mmb_id");
        dropColumnIfExists("pacs008", "instg_agt_mmb_id", "ALTER TABLE pacs008 DROP COLUMN instg_agt_mmb_id");
        dropColumnIfExists("pacs008", "instd_agt_mmb_id", "ALTER TABLE pacs008 DROP COLUMN instd_agt_mmb_id");
        dropColumnIfExists("pacs008", "dbtr_agt_mmb_id", "ALTER TABLE pacs008 DROP COLUMN dbtr_agt_mmb_id");
        dropColumnIfExists("pacs008", "cdtr_agt_mmb_id", "ALTER TABLE pacs008 DROP COLUMN cdtr_agt_mmb_id");
        // TEMPORARY FOR PAYAPT ADM.002 TESTING
        alterColumnIfExists("pacs008", "amount",
                "ALTER TABLE pacs008 MODIFY COLUMN amount DECIMAL(65, 2) NOT NULL");
        alterColumnIfExists("pacs008", "creditor_account",
                "ALTER TABLE pacs008 MODIFY COLUMN creditor_account VARCHAR(1024) NOT NULL");
        // END TEMPORARY
        addUniqueIndexIfMissing("pacs008", "uk_pacs008_transaction_id",
                "ALTER TABLE pacs008 ADD CONSTRAINT uk_pacs008_transaction_id UNIQUE (transaction_id)");
    }

    private void ensureBusinessMessageSequenceTable() {
        if (!tableExists("business_message_sequence")) {
            jdbcTemplate.execute("""
                    CREATE TABLE business_message_sequence (
                        sequence_date DATE NOT NULL,
                        sequence_value INT NOT NULL,
                        created_at DATETIME(6) NOT NULL,
                        updated_at DATETIME(6) NOT NULL,
                        PRIMARY KEY (sequence_date)
                    )
                    """);
        }
    }

    private void ensureSettlementTransactionTable() {
        if (!tableExists("settlement_transactions")) {
            jdbcTemplate.execute("""
                    CREATE TABLE settlement_transactions (
                        settlement_transaction_id BIGINT NOT NULL AUTO_INCREMENT,
                        payment_id BIGINT NOT NULL,
                        sender_account VARCHAR(255) NOT NULL,
                        beneficiary_account VARCHAR(1024) NOT NULL,
                        settlement_account VARCHAR(255) NOT NULL,
                        -- TEMPORARY FOR PAYAPT ADM.002 TESTING
                        amount DECIMAL(65, 2) NOT NULL,
                        -- END TEMPORARY
                        transaction_type VARCHAR(32) NOT NULL,
                        status VARCHAR(16) NOT NULL,
                        pacs008_message_id VARCHAR(22) NULL,
                        pacs002_status VARCHAR(16) NULL,
                        created_at DATETIME(6) NOT NULL,
                        updated_at DATETIME(6) NOT NULL,
                        PRIMARY KEY (settlement_transaction_id)
                    )
                    """);
        }
        // TEMPORARY FOR PAYAPT ADM.002 TESTING
        alterColumnIfExists("settlement_transactions", "beneficiary_account",
                "ALTER TABLE settlement_transactions MODIFY COLUMN beneficiary_account VARCHAR(1024) NOT NULL");
        alterColumnIfExists("settlement_transactions", "amount",
                "ALTER TABLE settlement_transactions MODIFY COLUMN amount DECIMAL(65, 2) NOT NULL");
        // END TEMPORARY
    }

    private void ensurePacs002Columns() {
        addColumnIfMissing("pacs002", "message_id", "ALTER TABLE pacs002 ADD COLUMN message_id VARCHAR(64) NULL AFTER original_message_id");
        addColumnIfMissing("pacs002", "transfer_id", "ALTER TABLE pacs002 ADD COLUMN transfer_id VARCHAR(35) NULL AFTER message_id");
        addColumnIfMissing("pacs002", "received_timestamp", "ALTER TABLE pacs002 ADD COLUMN received_timestamp DATETIME(6) NULL AFTER transaction_id");
    }

    private void ensureAdmi002Columns() {
        dropForeignKeysForColumn("adm002", "message_id");
        addColumnIfMissing("adm002", "message_id", "ALTER TABLE adm002 ADD COLUMN message_id VARCHAR(64) NULL AFTER adm002_id");
        addColumnIfMissing("adm002", "original_message_id", "ALTER TABLE adm002 ADD COLUMN original_message_id VARCHAR(64) NULL AFTER message_id");
        addColumnIfMissing("adm002", "original_reference", "ALTER TABLE adm002 ADD COLUMN original_reference VARCHAR(255) NULL AFTER adm002_id");
        addColumnIfMissing("adm002", "business_message_id", "ALTER TABLE adm002 ADD COLUMN business_message_id VARCHAR(64) NULL AFTER original_reference");
        alterColumnIfExists("adm002", "business_message_id",
                "ALTER TABLE adm002 MODIFY COLUMN business_message_id VARCHAR(64) NULL");
        addColumnIfMissing("adm002", "related_message_id", "ALTER TABLE adm002 ADD COLUMN related_message_id VARCHAR(64) NULL AFTER business_message_id");
        addColumnIfMissing("adm002", "error_code", "ALTER TABLE adm002 ADD COLUMN error_code VARCHAR(255) NULL AFTER related_message_id");
        addColumnIfMissing("adm002", "error_description", "ALTER TABLE adm002 ADD COLUMN error_description VARCHAR(1000) NULL AFTER error_code");
        addColumnIfMissing("adm002", "severity", "ALTER TABLE adm002 ADD COLUMN severity VARCHAR(64) NULL AFTER error_description");
        addColumnIfMissing("adm002", "creation_datetime", "ALTER TABLE adm002 ADD COLUMN creation_datetime DATETIME(6) NULL AFTER severity");
        addColumnIfMissing("adm002", "reject_reason_code", "ALTER TABLE adm002 ADD COLUMN reject_reason_code VARCHAR(255) NULL AFTER business_message_id");
        addColumnIfMissing("adm002", "reject_reason_description", "ALTER TABLE adm002 ADD COLUMN reject_reason_description VARCHAR(1000) NULL AFTER reject_reason_code");
        addColumnIfMissing("adm002", "rejection_date_time", "ALTER TABLE adm002 ADD COLUMN rejection_date_time DATETIME(6) NULL AFTER reject_reason_description");
        addColumnIfMissing("adm002", "xml_payload", "ALTER TABLE adm002 ADD COLUMN xml_payload TEXT NULL AFTER rejection_date_time");
        addColumnIfMissing("adm002", "transaction_id", "ALTER TABLE adm002 ADD COLUMN transaction_id BIGINT NULL AFTER xml_payload");
        addColumnIfMissing("adm002", "received_timestamp", "ALTER TABLE adm002 ADD COLUMN received_timestamp DATETIME(6) NULL AFTER transaction_id");
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
                    -- TEMPORARY FOR PAYAPT ADM.002 TESTING
                    amount DECIMAL(65, 2) NOT NULL,
                    -- END TEMPORARY
                    currency VARCHAR(3) NOT NULL,
                    debtor_name VARCHAR(140) NOT NULL,
                    debtor_account VARCHAR(255) NOT NULL,
                    debtor_town VARCHAR(35) NOT NULL,
                    debtor_country VARCHAR(2) NOT NULL,
                    creditor_name VARCHAR(140) NOT NULL,
                    creditor_account VARCHAR(1024) NOT NULL,
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
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE pacs002 (
                    pacs002_id BIGINT NOT NULL AUTO_INCREMENT,
                    original_message_id VARCHAR(255) NULL,
                    message_id VARCHAR(64) NULL,
                    transfer_id VARCHAR(35) NULL,
                    transaction_status VARCHAR(255) NULL,
                    reason_code VARCHAR(255) NULL,
                    xml_payload TEXT NULL,
                    transaction_id BIGINT NULL,
                    received_timestamp DATETIME(6) NULL,
                    PRIMARY KEY (pacs002_id)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE adm002 (
                    adm002_id BIGINT NOT NULL AUTO_INCREMENT,
                    message_id VARCHAR(64) NULL,
                    original_message_id VARCHAR(64) NULL,
                    original_reference VARCHAR(255) NULL,
                    business_message_id VARCHAR(64) NULL,
                    related_message_id VARCHAR(64) NULL,
                    error_code VARCHAR(255) NULL,
                    error_description VARCHAR(1000) NULL,
                    severity VARCHAR(64) NULL,
                    creation_datetime DATETIME(6) NULL,
                    reject_reason_code VARCHAR(255) NULL,
                    reject_reason_description VARCHAR(1000) NULL,
                    rejection_date_time DATETIME(6) NULL,
                    xml_payload TEXT NULL,
                    transaction_id BIGINT NULL,
                    received_timestamp DATETIME(6) NULL,
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

    private void alterColumnIfExists(String tableName, String columnName, String ddl) {
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

    private void expandColumnIfShorterThan(String tableName, String columnName, int minimumLength, String ddl) {
        if (!columnExists(tableName, columnName)) {
            return;
        }

        Integer length = jdbcTemplate.queryForObject("""
                SELECT CHARACTER_MAXIMUM_LENGTH
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, tableName, columnName);
        if (length != null && length < minimumLength) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void addUniqueIndexIfMissing(String tableName, String indexName, String ddl) {
        if (!indexExists(tableName, indexName)) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void addIndexIfMissing(String tableName, String indexName, String ddl) {
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
