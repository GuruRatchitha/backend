-- Fix for: Unknown column 'beneficiary_id' in 'beneficiary'
-- Run this on the same MySQL database configured in application.yml.

USE dev_db;

ALTER TABLE beneficiary
    ADD COLUMN beneficiary_id BIGINT NOT NULL AUTO_INCREMENT UNIQUE FIRST;
