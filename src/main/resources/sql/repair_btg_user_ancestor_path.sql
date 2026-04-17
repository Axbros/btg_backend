-- =============================================================================
-- 根据推荐链重建 btg_user.ancestor_path（MySQL 8.0+，需支持 WITH RECURSIVE）
-- 说明：应用内团队树在 MySQL 5.7 上通过 Java BFS + IN 查询实现，不依赖本脚本即可展示下级；
--       本脚本用于把 ancestor_path 列与其它依赖该列的逻辑对齐。
-- 执行前请备份；建议在维护窗口执行。
--
-- 规则与 Java AncestorPathUtil.buildChildAncestorPath 一致：
--   根用户（is_root=1）视为前缀 "/"
--   子用户 path = normalize(父 path) + 父 id + "/"
--
-- 锚点：所有 is_root=1 且未删除的用户。若库内无根用户，本脚本不会更新任何行。
-- =============================================================================

UPDATE btg_user u
INNER JOIN (
    WITH RECURSIVE computed AS (
        SELECT
            bu.id,
            bu.referrer_user_id,
            CAST('/' AS CHAR(2048)) AS ancestor_path_calc
        FROM btg_user bu
        WHERE bu.deleted_at IS NULL
          AND bu.is_root = 1

        UNION ALL

        SELECT
            bu.id,
            bu.referrer_user_id,
            CAST(
                CONCAT(
                    CASE
                        WHEN p.ancestor_path_calc IS NULL OR TRIM(p.ancestor_path_calc) = '' THEN '/'
                        WHEN p.ancestor_path_calc LIKE '%/' THEN p.ancestor_path_calc
                        ELSE CONCAT(p.ancestor_path_calc, '/')
                    END,
                    p.id,
                    '/'
                ) AS CHAR(2048)
            )
        FROM btg_user bu
        INNER JOIN computed p ON bu.referrer_user_id = p.id
        WHERE bu.deleted_at IS NULL
    )
    SELECT id, ancestor_path_calc
    FROM computed
) t ON u.id = t.id
SET u.ancestor_path = t.ancestor_path_calc;

-- 可选：检查仍有「推荐人不在本次计算闭包」的孤立行（需人工处理推荐关系）
-- SELECT id, mobile, referrer_user_id, ancestor_path FROM btg_user
-- WHERE deleted_at IS NULL AND referrer_user_id IS NOT NULL
--   AND referrer_user_id NOT IN (SELECT id FROM btg_user WHERE deleted_at IS NULL);
