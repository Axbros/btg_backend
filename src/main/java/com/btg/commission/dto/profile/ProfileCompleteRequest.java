package com.btg.commission.dto.profile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProfileCompleteRequest {

    /** 用户名（展示昵称，对应用户表 nickname） */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 100)
    private String nickname;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 100)
    private String realName;

    /** 可选；有值则写入资料表 */
    @Size(max = 100)
    private String idCardNo;

    /** 可选；不传则不改写库中原值，传空串可清空 */
    @Size(max = 500)
    private String idCardFrontUrl;

    /** 可选；同上 */
    @Size(max = 500)
    private String idCardBackUrl;

    /** 可选；同上 */
    @Size(max = 500)
    private String facePhotoUrl;

    @NotBlank(message = "服务器名称不能为空")
    @Size(max = 255)
    private String serverName;

    @NotBlank(message = "账户ID不能为空")
    @Size(max = 100)
    private String tradingAccountId;

    @NotBlank(message = "账户密码不能为空")
    @Size(max = 255)
    private String tradingAccountPassword;

    @NotBlank(message = "交易所Uid不能为空")
    @Size(max = 100)
    private String exchangeUid;

    @NotNull(message = "底仓本金不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "底仓本金不能为负")
    private BigDecimal principalAmount;
}
