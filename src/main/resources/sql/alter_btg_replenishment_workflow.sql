-- 补仓：管理员审核 → 转派资方执行人 → 资方凭证 → 申请人确认到账（最小补列；status 码迁移见下方 UPDATE）
-- 执行前请备份；与应用程序新版本一并部署。

ALTER TABLE btg_replenishment_apply
    ADD COLUMN assigned_capital_user_id BIGINT NULL DEFAULT NULL COMMENT '当前资方执行人' AFTER current_handler_user_id,
    ADD COLUMN assigned_by BIGINT NULL DEFAULT NULL COMMENT '转派人（管理员 user id）' AFTER assigned_capital_user_id,
    ADD COLUMN assigned_time DATETIME NULL DEFAULT NULL COMMENT '转派时间' AFTER assigned_by,
    ADD COLUMN assign_remark VARCHAR(500) NULL DEFAULT NULL COMMENT '转派备注' AFTER assigned_time,
    ADD COLUMN capital_submit_time DATETIME NULL DEFAULT NULL COMMENT '资方提交凭证时间' AFTER transfer_remark,
    ADD COLUMN capital_submit_remark VARCHAR(500) NULL DEFAULT NULL COMMENT '资方提交备注' AFTER capital_submit_time,
    ADD COLUMN capital_receiver_uid VARCHAR(100) NULL DEFAULT NULL COMMENT '资方收款UID' AFTER capital_submit_remark,
    ADD COLUMN arrival_confirm_status TINYINT NULL DEFAULT NULL COMMENT '到账确认状态：1待确认 2已确认到账 3拒绝到账' AFTER capital_receiver_uid,
    ADD COLUMN arrival_confirm_time DATETIME NULL DEFAULT NULL COMMENT '到账确认时间' AFTER arrival_confirm_status,
    ADD COLUMN arrival_confirm_by BIGINT NULL DEFAULT NULL COMMENT '到账确认人（申请人 user id）' AFTER arrival_confirm_time,
    ADD COLUMN arrival_confirm_remark VARCHAR(500) NULL DEFAULT NULL COMMENT '到账确认备注' AFTER arrival_confirm_by;

-- 旧 status 整型迁移为新状态机（见 ReplenishmentStatusEnum 注释）。仅处理未删除行。
UPDATE btg_replenishment_apply
SET status = CASE status
    WHEN 1 THEN 1
    WHEN 2 THEN 6
    WHEN 3 THEN 7
    WHEN 4 THEN 6
    WHEN 5 THEN 6
    WHEN 6 THEN 8
    WHEN 7 THEN 3
    WHEN 8 THEN 4
    WHEN 9 THEN 7
    ELSE status
END
WHERE deleted_at IS NULL;

-- 原「待申请人确认」类数据：标记到账确认为待确认
UPDATE btg_replenishment_apply
SET arrival_confirm_status = 1
WHERE deleted_at IS NULL
  AND status = 4
  AND transfer_screenshot_url IS NOT NULL;
