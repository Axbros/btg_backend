package com.btg.commission.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadVo {

    /**
     * 可直接用于资料接口、img src 的完整 URL
     */
    private String url;

    /** 通用上传不持久化元数据时为 null；利润申报附件见申报接口 */
    private Long attachmentId;

    private String originalFilename;

    private String fileType;
}
