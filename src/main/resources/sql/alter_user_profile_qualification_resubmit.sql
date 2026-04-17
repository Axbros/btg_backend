-- 资格审核：拒绝后重提次数与最近提交时间（audit_log 仍保留各轮历史；本表字段便于列表/详情展示与统计）
ALTER TABLE btg_user_profile
    ADD COLUMN qualification_submit_count INT NOT NULL DEFAULT 1 COMMENT '资格审核提交次数（注册/首条资料行为 1，每次用户重提 +1）' AFTER qualification_audit_remark,
    ADD COLUMN qualification_last_submit_time DATETIME NULL DEFAULT NULL COMMENT '最近一次用户提交/重提资格审核时间' AFTER qualification_submit_count;
