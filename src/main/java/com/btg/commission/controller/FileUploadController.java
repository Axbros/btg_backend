package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.enums.FileAttachmentFileType;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.LocalFileStorageService;
import com.btg.commission.vo.FileUploadVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final LocalFileStorageService localFileStorageService;

    @Operation(summary = "上传文件到本地磁盘", description = "返回可公网/内网访问的完整 URL（依赖 btg.file.public-base-url）。文件通过 GET /files/... 提供访问。")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<FileUploadVo> upload(
            @RequestPart("file") @Parameter(description = "文件", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                    schema = @Schema(type = "string", format = "binary"))) MultipartFile file,
            @RequestParam(value = "type", required = false, defaultValue = "OTHER")
            @Parameter(description = "业务文件类型：ID_CARD_FRONT / ID_CARD_BACK / FACE / PROFIT / TRANSFER / OTHER")
            FileAttachmentFileType type) {
        return ApiResult.ok(localFileStorageService.storeForUser(file, SecurityUtils.requireUserId(), type));
    }
}
