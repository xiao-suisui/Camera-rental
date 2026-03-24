package com.example.camerarental.device.controller;

import com.example.camerarental.common.enums.ErrorCode;
import com.example.camerarental.common.exception.BusinessException;
import com.example.camerarental.common.result.Result;
import com.example.camerarental.common.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

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
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        validateFile(file);
        try {
            String url = minioService.uploadFile(file);
            return Result.success(url);
        } catch (Exception e) {
            log.error("图片上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败，请稍后重试");
        }
    }

    @PostMapping("/images")
    public Result<List<String>> uploadImages(@RequestParam("files") MultipartFile[] files) {
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
    }
}
