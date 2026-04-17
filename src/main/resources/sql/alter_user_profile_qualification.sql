-- 新成员资格审核：挂在 btg_user_profile（资料与审核字段同表，改动最小；btg_user 已有 status 表示资料/上级审核链，避免混用）
-- 执行前请自行备份。

ALTER TABLE btg_user_profile
    ADD COLUMN qualification_status TINYINT NOT NULL DEFAULT 1 COMMENT '资格审核状态：1待审核 2已通过 3已拒绝' AFTER principal_amount,
    ADD COLUMN qualification_audit_time DATETIME NULL DEFAULT NULL COMMENT '资格审核时间' AFTER qualification_status,
    ADD COLUMN qualification_audit_by BIGINT NULL DEFAULT NULL COMMENT '资格审核人（管理员 user id）' AFTER qualification_audit_time,
    ADD COLUMN qualification_audit_remark VARCHAR(500) NULL DEFAULT NULL COMMENT '资格审核备注' AFTER qualification_audit_by;

-- 历史数据：已有资料用户视为已通过，避免上线后全员无法登录
UPDATE btg_user_profile
SET qualification_status = 2
WHERE deleted_at IS NULL;
