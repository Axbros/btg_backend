package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页数据（与团队列表等接口一致）")
public class PageVo<T> {

    @Schema(description = "当前页记录")
    private List<T> records;

    @Schema(description = "总条数")
    private long total;

    @Schema(description = "当前页码，从 1 开始")
    private long page;

    @Schema(description = "每页条数")
    private long pageSize;
}
