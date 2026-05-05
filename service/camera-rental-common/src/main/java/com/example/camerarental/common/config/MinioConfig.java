package com.example.camerarental.common.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketCorsArgs;
import io.minio.messages.CORSConfiguration;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
@ConditionalOnClass(MinioClient.class)
public class MinioConfig {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;

    private MinioClient client;

    @Bean
    public MinioClient minioClient() {
        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        return this.client;
    }

    @PostConstruct
    public void initBucketCors() {
        if (client == null) {
            return;
        }
        try {
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                client.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket created: {}", bucketName);
            }

            CORSConfiguration.CORSRule rule = new CORSConfiguration.CORSRule(
                    List.of("*"),
                    List.of("GET", "HEAD", "PUT", "POST", "DELETE"),
                    List.of("*"),
                    null,
                    null,
                    3600
            );
            CORSConfiguration cors = new CORSConfiguration(List.of(rule));
            client.setBucketCors(
                    SetBucketCorsArgs.builder().bucket(bucketName).config(cors).build());
            log.info("MinIO CORS configured for bucket: {}", bucketName);
        } catch (Exception e) {
            log.warn("Failed to configure MinIO CORS (bucket may already have CORS): {}", e.getMessage());
        }
    }
}

