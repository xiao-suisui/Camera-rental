package com.example.camerarental.common.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(MinioClient.class)
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.signed-url-expiry-seconds:900}")
    private Integer signedUrlExpirySeconds;

    /**
     * 上传文件
     */
    public String uploadFile(MultipartFile file) throws Exception {
        // 检查桶是否存在
        boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );

        if (!bucketExists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
            );
            log.info("Bucket created: {}", bucketName);
        }

        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;

        // 上传文件
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        }

        log.info("File uploaded: {}", fileName);

        // 返回短期签名URL（对象默认私有）
        return getFileUrl(fileName);
    }

    /**
     * 获取文件访问URL
     */
    public String getFileUrl(String fileName) throws Exception {
        int expiry = Math.max(60, signedUrlExpirySeconds == null ? 900 : signedUrlExpirySeconds);
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(normalizeToObjectKey(fileName))
                        .expiry(expiry, TimeUnit.SECONDS)
                        .build()
        );
    }

    /**
     * 将 image_paths（逗号分隔）转换为短期签名URL（逗号分隔）
     */
    public String toSignedUrlPaths(String imagePaths) {
        if (imagePaths == null || imagePaths.isBlank()) {
            return imagePaths;
        }
        List<String> signed = Arrays.stream(imagePaths.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(path -> {
                    try {
                        return getFileUrl(path);
                    } catch (Exception ex) {
                        log.warn("Failed to sign image path: {}", path, ex);
                        return path;
                    }
                })
                .collect(Collectors.toList());
        return String.join(",", signed);
    }

    /**
     * 将 image_paths（逗号分隔）规范化为对象键（逗号分隔）
     */
    public String normalizeImagePaths(String imagePaths) {
        if (imagePaths == null || imagePaths.isBlank()) {
            return imagePaths;
        }
        List<String> keys = Arrays.stream(imagePaths.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::normalizeToObjectKey)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        return String.join(",", keys);
    }

    /**
     * 将前端提交的 URL 或对象键统一转换为对象键。
     */
    public String normalizeToObjectKey(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) {
            return pathOrUrl;
        }

        String value = pathOrUrl.trim();
        int q = value.indexOf('?');
        if (q >= 0) {
            value = value.substring(0, q);
        }

        if (value.startsWith("http://") || value.startsWith("https://")) {
            try {
                URI uri = URI.create(value);
                String path = uri.getPath();
                if (path != null && !path.isBlank()) {
                    String normalized = path.startsWith("/") ? path.substring(1) : path;
                    String bucketPrefix = bucketName + "/";
                    if (normalized.startsWith(bucketPrefix)) {
                        return normalized.substring(bucketPrefix.length());
                    }
                    return normalized;
                }
            } catch (Exception ex) {
                log.warn("Invalid minio url format: {}", pathOrUrl, ex);
            }
        }

        String bucketPrefix = bucketName + "/";
        if (value.startsWith(bucketPrefix)) {
            return value.substring(bucketPrefix.length());
        }
        return value.startsWith("/") ? value.substring(1) : value;
    }

    /**
     * 删除文件
     */
    public void deleteFile(String fileName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build()
        );
        log.info("File deleted: {}", fileName);
    }
}

