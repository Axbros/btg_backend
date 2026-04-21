package com.btg.commission.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "管理端更新 WebView 网关配置（单条）")
public class WebviewConfigUpdateDTO {

    @NotNull(message = "enabled 不能为空")
    @Schema(description = "是否启用")
    private Boolean enabled;

    @NotBlank(message = "webUrl 不能为空")
    @Size(max = 500, message = "webUrl 最长 500 字符")
    @Schema(description = "H5 地址，须为 https://")
    private String webUrl;

    @Size(max = 20000, message = "injectJs 最长 20000 字符")
    private String injectJs;

    @Size(max = 20000, message = "injectCss 最长 20000 字符")
    private String injectCss;

    @NotNull(message = "showSplash 不能为空")
    private Boolean showSplash;

    @NotNull(message = "splashDurationMs 不能为空")
    @Min(value = 0, message = "splashDurationMs 不能为负")
    private Integer splashDurationMs;

    @NotNull(message = "version 不能为空")
    @Min(value = 1, message = "version 须 >= 1")
    @Schema(description = "配置版本号（建议每次发布递增）")
    private Long version;

    @Size(max = 500)
    private String remark;
}
