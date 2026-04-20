-- 由「单行 id=1 覆盖」改为「按 business_date 每日一行」；在已存在旧表时执行（按需调整后再跑）
-- 1) 若无 deleted_at，先执行 alter_btg_profit_report_window_deleted_at.sql

-- 2) 旧占位行若 business_date 为空，可先删掉或补日期（补日期须与 uk 不冲突）
-- DELETE FROM btg_profit_report_window WHERE business_date IS NULL;

-- 3) 主键改为自增（MySQL 8+）
ALTER TABLE btg_profit_report_window
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键';

-- 4) business_date 改为非空（请先保证无 NULL）
UPDATE btg_profit_report_window SET business_date = CURDATE() WHERE business_date IS NULL;

ALTER TABLE btg_profit_report_window
    MODIFY COLUMN business_date DATE NOT NULL COMMENT '窗口自然日（Asia/Shanghai）';

-- 5) 每日唯一
ALTER TABLE btg_profit_report_window
    ADD UNIQUE KEY uk_business_date (business_date);
