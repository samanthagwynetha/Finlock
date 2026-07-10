-- Change balance column from NUMERIC to VARCHAR to store AES-256-GCM encrypted values
ALTER TABLE wallets ALTER COLUMN balance TYPE VARCHAR(255);
