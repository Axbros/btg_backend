-- 分润：兜底/不兜底双比例 + 当前生效模式；利润单与分润明细快照 commission_mode
-- 执行前请备份

ALTER TABLE `btg_user_profit_config`
    ADD COLUMN `guarantee_ratio` decimal(10,6) NOT NULL DEFAULT 0.000000 COMMENT '兜底模式：子级可分总利润比例' AFTER `child_profit_ratio`,
    ADD COLUMN `non_guarantee_ratio` decimal(10,6) NOT NULL DEFAULT 0.000000 COMMENT '不兜底模式：子级可分总利润比例' AFTER `guarantee_ratio`,
    ADD COLUMN `commission_mode` varchar(32) NOT NULL DEFAULT 'GUARANTEE' COMMENT '当前生效分润模式：GUARANTEE兜底 NON_GUARANTEE不兜底' AFTER `non_guarantee_ratio`;

UPDATE `btg_user_profit_config`
SET `guarantee_ratio` = `child_profit_ratio`,
    `non_guarantee_ratio` = `child_profit_ratio`,
    `commission_mode` = 'GUARANTEE'
WHERE `deleted_at` IS NULL;

ALTER TABLE `btg_profit_report`
    ADD COLUMN `commission_mode` varchar(32) DEFAULT NULL COMMENT '本次利润单使用的分润模式快照' AFTER `profit_amount`;

ALTER TABLE `btg_profit_distribution`
    ADD COLUMN `commission_mode` varchar(32) DEFAULT NULL COMMENT '本次分润明细使用的分润模式快照' AFTER `report_id`;
