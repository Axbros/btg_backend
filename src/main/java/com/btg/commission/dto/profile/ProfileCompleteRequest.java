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

    /**
     * 可选。若填写必须与当前登录账号手机号一致；本接口不会修改绑定的手机号。
     */
    @Size(max = 20)
    private String mobile;

    /** 可选 */
    @Size(max = 100)
    private String realName;

    /** 可选 */
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

    /** 库中尚无密码时必填；已有密码时不传或空串则保留原值 */
    @Size(max = 255)
    private String tradingAccountPassword;

    @NotBlank(message = "交易所Uid不能为空")
    @Size(max = 100)
    private String exchangeUid;

    @NotBlank(message = "券商名称不能为空")
    @Size(max = 255)
    private String walletName;

    @NotBlank(message = "钱包地址不能为空")
    @Size(max = 512)
    private String walletAddress;

    @NotNull(message = "底仓本金不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "底仓本金不能为负")
    private BigDecimal principalAmount;

    /** 可选；null 表示不修改，传空串可清空已保存的 Key */
    @Size(max = 256)
    private String bitgetAccessKey;

    /** 可选；null 表示不修改，传空串可清空 */
    @Size(max = 256)
    private String bitgetSecretKey;

    /** 可选；null 表示不修改，传空串可清空 */
    @Size(max = 256)
    private String bitgetPassphrase;
}
