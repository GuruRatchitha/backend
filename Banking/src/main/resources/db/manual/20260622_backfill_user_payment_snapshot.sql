-- Fix for payment error: Sender town name is required.
-- Run this on the same MySQL database configured in application.yml.

USE dev_db;

UPDATE users
SET
    country_code = COALESCE(NULLIF(country_code, ''), 'us'),
    town_name = COALESCE(NULLIF(town_name, ''), 'Fountain Hills')
WHERE town_name IS NULL
   OR town_name = ''
   OR country_code IS NULL
   OR country_code = '';
