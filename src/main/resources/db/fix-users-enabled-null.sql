-- Chạy thủ công nếu cần (MySQL cd_web)
UPDATE users SET enabled = 1 WHERE enabled IS NULL;
ALTER TABLE users MODIFY COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1;
