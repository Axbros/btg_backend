-- 归仓申请：关联补仓执行方（资方审核人）与收款 UID 快照；与补仓 assigned_capital_user_id / capital_receiver_uid 对齐。
-- 执行前请确认列不存在。

ALTER TABLE btg_replenishment_repay_apply
    ADD COLUMN capital_user_id BIGINT DEFAULT NULL COMMENT '补仓执行方/归仓审核人（=补仓单 assigned_capital_user_id 快照）' AFTER user_id,
    ADD COLUMN capital_receiver_uid VARCHAR(100) DEFAULT NULL COMMENT '补仓执行方收款 UID 快照' AFTER capital_user_id;

-- 历史数据回填（按关联补仓单）
UPDATE btg_replenishment_repay_apply r
INNER JOIN btg_replenishment_apply p ON r.replenish_apply_id = p.id AND p.deleted_at IS NULL
SET r.capital_user_id = p.assigned_capital_user_id,
    r.capital_receiver_uid = p.capital_receiver_uid
WHERE r.deleted_at IS NULL
  AND r.capital_user_id IS NULL
  AND p.assigned_capital_user_id IS NOT NULL;

-- 可选：与枚举名一致
UPDATE btg_replenishment_repay_apply
SET flow_status = 'PENDING_CAPITAL_REVIEW'
WHERE deleted_at IS NULL
  AND flow_status = 'PENDING_AUDIT';
