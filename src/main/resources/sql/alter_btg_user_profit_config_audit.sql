ALTER TABLE `btg_user_profit_config`
    ADD COLUMN `audit_status` enum('PENDING','APPROVED','REJECTED') COLLATE utf8mb4_unicode_ci NULL COMMENT '审核状态' AFTER `deleted_at`,
    ADD COLUMN `audit_time` datetime DEFAULT NULL COMMENT '审核时间' AFTER `audit_status`,
    ADD COLUMN `auditor_id` bigint(20) DEFAULT NULL COMMENT '审核人（根用户ID）' AFTER `audit_time`;

UPDATE `btg_user_profit_config`
SET `audit_status` = 'APPROVED'
WHERE `audit_status` IS NULL;

ALTER TABLE `btg_user_profit_config`
    MODIFY COLUMN `audit_status` enum('PENDING','APPROVED','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '审核状态';
