package com.example.camerarental.device.controller;

import com.example.camerarental.common.enums.ErrorCode;
import com.example.camerarental.common.exception.BusinessException;
import com.example.camerarental.common.result.Result;
import com.example.camerarental.common.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 图片上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/device/upload")
@RequiredArgsConstructor
public class UploadController {

    private final MinioService minioService;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/jpg", "image/png", "image/webp");

    @PostMapping("/image")
    public Result<String> uploadImage(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("file") MultipartFile file) {
        validateFile(file);
        try {
            String url = minioService.uploadFile(file);
            log.info("Image uploaded by userId={}", userId);
            return Result.success(url);
        } catch (Exception e) {
            log.error("图片上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败，请稍后重试");
        }
    }

    @PostMapping("/images")
    public Result<List<String>> uploadImages(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("files") MultipartFile[] files) {
        if (files.length > 9) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "最多上传9张图片");
        }
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            validateFile(file);
            try {
                urls.add(minioService.uploadFile(file));
            } catch (Exception e) {
                log.error("图片上传失败: {}", file.getOriginalFilename(), e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败: " + file.getOriginalFilename());
            }
        }
        log.info("Images uploaded: count={}, userId={}", urls.size(), userId);
        return Result.success(urls);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件大小不能超过5MB");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持 jpg/png/webp 格式");
        }
        // 校验文件魔数（magic bytes），防止 Content-Type 伪造
        if (!isValidImageMagicBytes(file)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容与声明的类型不匹配");
        }
    }

    private boolean isValidImageMagicBytes(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 4) return false;

            // JPEG: FF D8 FF
            if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) return true;
            // PNG: 89 50 4E 47
            if ((header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) return true;
            // GIF: 47 49 46 38
            if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38) return true;
            // WEBP: RIFF....WEBP
            if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46 && read >= 8
                    && header[4] == 0x57 && header[5] == 0x45 && header[6] == 0x42 && header[7] == 0x50) return true;

            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
