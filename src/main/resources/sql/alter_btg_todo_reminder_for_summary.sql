ALTER TABLE `btg_todo_reminder`
    ADD COLUMN `todo_type` varchar(64) COLLATE utf8mb4_unicode_ci NULL COMMENT '统一待办类型' AFTER `status`,
    ADD COLUMN `business_type` varchar(32) COLLATE utf8mb4_unicode_ci NULL COMMENT '业务域：settlement/profit_report/replenishment/repay/qualification' AFTER `todo_type`,
    ADD COLUMN `business_id` bigint(20) NULL COMMENT '业务主键ID' AFTER `business_type`,
    ADD COLUMN `owner_user_id` bigint(20) NULL COMMENT '待办归属用户' AFTER `business_id`,
    ADD COLUMN `reminder_state` varchar(16) COLLATE utf8mb4_unicode_ci NULL COMMENT 'OPEN / DONE / CANCELLED' AFTER `owner_user_id`,
    ADD COLUMN `source_status` varchar(64) COLLATE utf8mb4_unicode_ci NULL COMMENT '源单状态快照' AFTER `reminder_state`,
    ADD COLUMN `source_updated_at` datetime NULL COMMENT '源单更新时间快照' AFTER `source_status`,
    ADD COLUMN `dedupe_key` varchar(128) COLLATE utf8mb4_unicode_ci NULL COMMENT '幂等去重键' AFTER `source_updated_at`,
    ADD COLUMN `resolved_at` datetime NULL COMMENT '关闭时间' AFTER `dedupe_key`;

UPDATE `btg_todo_reminder`
SET
    `todo_type` = COALESCE(`todo_type`, `task_type`),
    `business_type` = COALESCE(`business_type`, 'legacy'),
    `business_id` = COALESCE(`business_id`, `related_id`),
    `owner_user_id` = COALESCE(`owner_user_id`, `user_id`),
    `reminder_state` = COALESCE(`reminder_state`, CASE WHEN `status` = 'COMPLETED' THEN 'DONE' ELSE 'OPEN' END),
    `dedupe_key` = COALESCE(`dedupe_key`, CONCAT(COALESCE(`task_type`, 'LEGACY'), ':legacy:', COALESCE(`related_id`, 0), ':', COALESCE(`user_id`, 0)))
WHERE `todo_type` IS NULL
   OR `business_type` IS NULL
   OR `business_id` IS NULL
   OR `owner_user_id` IS NULL
   OR `reminder_state` IS NULL
   OR `dedupe_key` IS NULL;

ALTER TABLE `btg_todo_reminder`
    MODIFY COLUMN `todo_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '统一待办类型',
    MODIFY COLUMN `business_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务域：settlement/profit_report/replenishment/repay/qualification',
    MODIFY COLUMN `business_id` bigint(20) NOT NULL COMMENT '业务主键ID',
    MODIFY COLUMN `owner_user_id` bigint(20) NOT NULL COMMENT '待办归属用户',
    MODIFY COLUMN `reminder_state` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN / DONE / CANCELLED',
    MODIFY COLUMN `dedupe_key` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '幂等去重键';

ALTER TABLE `btg_todo_reminder`
    ADD KEY `idx_dedupe_state` (`dedupe_key`, `reminder_state`),
    ADD KEY `idx_owner_open` (`owner_user_id`, `reminder_state`, `todo_type`),
    ADD KEY `idx_business` (`business_type`, `business_id`);
