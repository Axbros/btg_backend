# BTG 推荐分佣后端

Spring Boot 3.2 + Java 17 + MyBatis-Plus + Spring Security + JWT。

## 准备

1. 创建数据库并执行仓库根目录的 `btg_commission.sql`。
2. 修改 `src/main/resources/application.yml` 中的数据源与 `btg.jwt.secret`（生产环境须使用足够长的随机密钥）。

## 运行

```bash
cd btg-commission-backend
mvn spring-boot:run
```

默认端口：`8080`。

## API 文档（Swagger）

启动后访问：

- **Swagger UI**：<http://localhost:8080/swagger-ui.html>
- **OpenAPI JSON**：<http://localhost:8080/v3/api-docs>

在 Swagger 页面点击 **Authorize**，输入 `Bearer <登录返回的 token>`（或直接粘贴 token，部分版本会自动加 Bearer）即可调试需登录的接口。

## 统一返回格式

成功时 HTTP 状态码一般为 200，响应体为：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

失败时 `code` 为业务或 HTTP 语义码（如 400、401、404、409、500），`message` 为具体说明，`data` 多为 `null`。

## 构建说明

若本机使用 **JDK 24+** 编译，项目已固定 **Lombok 1.18.38**（见 `pom.xml` 的 `lombok.version`），避免 `TypeTag :: UNKNOWN` 编译错误。

## 初始数据与登录

用户数据表为 **`btg_user`**。脚本中根用户的 `password_hash` 若为 BCrypt，可用对应明文登录；否则请使用 `POST /api/v1/auth/register` 注册新用户，或在库中用 BCrypt 更新 `btg_user.password_hash`。

根用户 `is_root=1` 拥有 `ROLE_ADMIN`，可访问 `/api/admin/**`（审核收益申报、维护策略等）。

## 主要接口（均需 `Authorization: Bearer <token>`，除注册登录外）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册；**邀请码必填**，须为已存在用户的有效邀请码 |
| POST | `/api/auth/login` | 登录，返回 JWT |
| GET | `/api/user/me` | 当前用户信息（含 `referrerNickname`、`kycStatus`；无资料行时 `kycStatus` 为 null） |
| GET | `/api/user/{id}` | 按用户 ID 查看用户：`user`、`profile`（无则 null）；另含与直属上级有效绑定的 `strategyId`、`strategyName`、`commissionRate`（无绑定或策略已删时相关字段为 null） |
| PUT | `/api/user/profile` | 完善资料（昵称、实名、交易账户与底仓本金等）；**身份证正反面、人脸 URL 可选**（不传不改库内原值） |
| POST | `/api/files/upload` | 上传文件到本地磁盘，返回可访问 URL（`multipart/form-data`，字段 `file`，可选 `type`） |
| GET | `/api/user/team/direct` | 直属下级分页：查询参数 `page`（默认 1）、`pageSize`（默认 10，最大 100）；`data` 为 `{ records, total, page, pageSize }`，`records` 项含 `id`、`mobile`、`kycStatus` |
| GET | `/api/user/team/descendants` | 全部下级分页，参数与返回结构同上 |
| GET | `/api/me/account-summary` | 我的账户汇总（BigDecimal） |
| GET | `/api/me/team-stats` | 团队人数统计 |
| GET | `/api/me/commission-strategy` | 本人当前分佣策略快照；可选 `profitAmount`：`previewCommissionAmount`/`previewTransferAmount`=盈利×(1−比例)（分给上级）；`previewNetAmount`=盈利×比例（本人自留） |
| GET | `/api/strategies` | 启用中的分佣策略 |
| POST | `/api/bindings` | 推荐人为直属下级绑定策略；**下级 `kycStatus` 须为已通过（2）** |
| POST | `/api/kyc/audit/approve` | KYC 审核通过；`body`: `{ "targetUserId", "remark?" }`；操作人须为对方**直属上级或任意上级** |
| POST | `/api/kyc/audit/reject` | KYC 审核拒绝；权限同上 |
| POST | `/api/profits/submit` | 收益申报 |
| GET | `/api/profits/mine` | 我的收益申报分页 |
| GET | `/api/profits/referrer/records` | **直属上级**查看下级收益申报分页；`page`、`pageSize`；可选 `status`：`PENDING`/`APPROVED`/`REJECTED`；列表 `records` 为精简项（见下「响应结构」） |
| GET | `/api/profits/referrer/records/{id}` | 同上权限下的单条详情；返回 `userNickname`、`strategyName`，不返回 `userId` / `referrerUserId` / `strategyId` |
| POST | `/api/profits/referrer/approve` | 直属上级同意；`body` 同 `{ "profitRecordId", "remark?" }` |
| POST | `/api/profits/referrer/reject` | 直属上级拒绝；**上上级、非申报人直属的根用户不可审** |
| GET | `/api/commissions/mine` | 我的佣金流水分页；`records` 每项仅 `id`、`profitRecordNo`、`profitAmount`、`commissionAmount`、`commissionRate`、`status` |
| GET | `/api/commissions/mine/{id}` | 同上列表中某条详情（须为当前用户收款流水）；含 `profitRecordId`、`fromUserId`、`toUserId`、`strategyId`、`confirmedTime`、`fromNickname`、`fromMobile`、`strategyName` 等 |
| GET | `/api/admin/profits/pending` | 待审核列表（管理员） |
| POST | `/api/admin/profits/approve` | 审核通过（事务 + 行锁 + 幂等） |
| POST | `/api/admin/profits/reject` | 审核拒绝（不生成佣金流水） |
| GET/POST/PUT | `/api/admin/strategies` | 策略管理（管理员） |

### 直属上级审核收益申报 · 响应结构

四层统一外壳均为 **`{ "code": 200, "message": "success", "data": ... }`**（失败时 `code`/`message` 变化，`data` 多为 `null`）。

1. **`GET /api/profits/referrer/records`** — `data` 为分页体 **`PageVo<ReferrerProfitListItemVo>`**（`records` 每项含：`id`、`recordNo`、`submitTime`、`userMobile`、`profitAmount`、`commissionRate`、`dueShareAmount`（应分佣）、`netAmount`（净额）、`status`）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "recordNo": "PR202504101200001234",
        "submitTime": "2025-04-10T12:00:00",
        "userMobile": "13800138000",
        "profitAmount": "200.00",
        "commissionRate": "0.4000",
        "dueShareAmount": "120.00",
        "netAmount": "80.00",
        "status": 1
      }
    ],
    "total": 10,
    "page": 1,
    "pageSize": 10
  }
}
```

2. **`GET /api/profits/referrer/records/{id}`** — `data` 为 **`ReferrerProfitRecordDetailVo`**（含 `userNickname`、`strategyName`；不含 `userId`、`referrerUserId`、`strategyId`；其余与金额、截图、状态、审核字段同原申报单）。

3. **`POST /api/profits/referrer/approve`**、**`POST /api/profits/referrer/reject`** — 成功时 **`data` 为 `null`**：
```json
{ "code": 200, "message": "success", "data": null }
```

Swagger 中对应展示模型：`ReferrerProfitListApiResponse`、`ReferrerProfitDetailApiResponse`、`ReferrerProfitAuditApiResponse`（包 `com.btg.commission.openapi`）。

## 本地文件上传

- 配置项：`btg.file.upload-dir`（存储目录）、`btg.file.public-base-url`（拼进返回的 `url`，部署在反代后请改为对外域名）、`btg.file.max-file-size-bytes`。
- 上传接口需登录：`POST /api/files/upload`，表单字段 `file`；可选查询参数 `type`：`ID_CARD_FRONT`、`ID_CARD_BACK`、`FACE`、`PROFIT`、`TRANSFER`、`OTHER`（默认）。
- 访问地址：`GET {public-base-url}/files/{yyyy/MM/dd}/{uuid}.ext`，映射到本地 `upload-dir` 下相同相对路径；通用上传不落库。利润单附件在 `btg_profit_attachment`（随申报写入）。

## 业务要点

- **分佣策略绑定**：仅当下级资料 `kyc_status = 2`（已通过）时允许绑定；否则返回业务冲突提示。
- **KYC 审核**：资料完善后进入待审核（1）时，**直属上级与任意上级**（由 `ancestor_path` + `referrer_user_id` 判定）可调用通过/拒绝接口；审核写 `btg_audit_log`（`USER_PROFILE_KYC`）。
- **收益审核**：`/api/profits/referrer/*` 要求操作人 **等于** 申报单 `referrer_user_id`（仅直属上级）；`/api/admin/profits/*` 仍为管理员全量审核。
- **收益申报提交（申报人）**：`pending_commission_out_amount` += `commission_amount`（总盈利×(1−分佣比例)，分给上级）；`pending_commission_in_amount` += `net_amount`（总盈利×分佣比例，本人自留待审）。**上级**：`pending_commission_in_amount` += `commission_amount`（收下级的分出部分）。
- **收益审核通过**：核销上述三处 pending；下级 `total_profit_amount` += `net_amount`（累积盈利×比例）；上级 `total_profit_amount` 与 `total_commission_in_amount` += `commission_amount`（累积盈利×(1−比例)、收到佣金）；下级 `total_commission_out_amount` += `commission_amount`。`btg_commission_record.commission_amount` = `commission_amount`。幂等同前。
- **收益申报单**：`commission_amount` = 总盈利×(1−分佣比例)（分出/上级应收）；`net_amount` = 总盈利×分佣比例（申报人自留）。
- **金额**：实体与汇总均使用 `BigDecimal`；状态字段使用枚举持久化为整型。
