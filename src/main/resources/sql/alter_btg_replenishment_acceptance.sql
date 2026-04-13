-- 补仓：资方受理、申请人补充资料；状态码 7/8 见 ReplenishmentStatusEnum
ALTER TABLE btg_replenishment_apply
    ADD COLUMN accepted_at datetime NULL COMMENT '资方受理时间' AFTER audit_remark,
    ADD COLUMN accepted_by bigint NULL COMMENT '资方受理人 user id' AFTER accepted_at,
    ADD COLUMN supplement_screenshot_url varchar(500) NULL COMMENT '可选历史字段；新流程资方凭证使用 transfer_screenshot_url' AFTER accepted_by;
