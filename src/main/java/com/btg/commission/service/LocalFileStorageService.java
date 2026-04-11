package com.btg.commission.service;

import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.config.FileStorageProperties;
import com.btg.commission.entity.FileAttachment;
import com.btg.commission.enums.FileAttachmentBusinessType;
import com.btg.commission.enums.FileAttachmentFileType;
import com.btg.commission.mapper.FileAttachmentMapper;
import com.btg.commission.vo.FileUploadVo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "pdf");

    private static final DateTimeFormatter DAY_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final FileStorageProperties fileStorageProperties;
    private final FileAttachmentMapper fileAttachmentMapper;

    @PostConstruct
    public void ensureUploadRoot() throws IOException {
        Files.createDirectories(fileStorageProperties.uploadRoot());
    }

    @Transactional(rollbackFor = Exception.class)
    public FileUploadVo storeForUser(MultipartFile file, Long userId, FileAttachmentFileType fileType) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "请选择文件");
        }
        if (file.getSize() > fileStorageProperties.getMaxFileSizeBytes()) {
            throw new BizException(ResultCode.BAD_REQUEST, "文件过大");
        }

        String original = file.getOriginalFilename();
        String ext = resolveExtension(original);
        if (ext == null || !ALLOWED_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT))) {
            throw new BizException(ResultCode.BAD_REQUEST, "仅支持 jpg、jpeg、png、webp、gif、pdf");
        }

        String dayFolder = LocalDate.now().format(DAY_PATH);
        String storedName = UUID.randomUUID() + "." + ext.toLowerCase(Locale.ROOT);
        String relative = dayFolder + "/" + storedName;

        Path root = fileStorageProperties.uploadRoot();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new BizException(ResultCode.BAD_REQUEST, "非法路径");
        }

        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "文件保存失败");
        }

        String base = fileStorageProperties.getPublicBaseUrl().replaceAll("/+$", "");
        String url = base + "/files/" + relative.replace('\\', '/');

        FileAttachment row = new FileAttachment();
        row.setBusinessType(FileAttachmentBusinessType.USER_PROFILE.getCode());
        row.setBusinessId(userId);
        row.setFileType(fileType.getCode());
        row.setFileUrl(url);
        row.setFileName(StringUtils.hasText(original) ? original : storedName);
        fileAttachmentMapper.insert(row);

        return FileUploadVo.builder()
                .url(url)
                .attachmentId(row.getId())
                .originalFilename(original)
                .fileType(fileType.getCode())
                .build();
    }

    private String resolveExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        String ext = filename.substring(dot + 1).trim();
        if (ext.isEmpty() || ext.contains("/") || ext.contains("\\") || ext.contains("..")) {
            return null;
        }
        return ext;
    }
}
