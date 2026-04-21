package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(description = "用户指派下拉项：仅 id 与昵称")
public class UserPickerOptionVO {

    Long id;

    String nickname;
}
