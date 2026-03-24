package com.example.camerarental.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 统一基础配置（放在 common 模块）
 *
 * <p>各业务服务只需在 application.yml 中覆盖以下属性即可定制文档标题和描述：
 * <pre>
 * api-doc:
 *   title: "User Service API"
 *   description: "用户服务 API 文档"
 * </pre>
 *
 * <p>所有服务统一携带 JWT Bearer Token 认证方案。
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class OpenApiConfig {

    @Value("${api-doc.title:Camera Rental Platform API}")
    private String title;

    @Value("${api-doc.description:相机租赁平台 RESTful API 文档}")
    private String description;

    @Value("${api-doc.version:v1.0.0}")
    private String version;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .description(description)
                        .version(version)
                        .contact(new Contact()
                                .name("Camera Rental Team")
                                .email("support@camera-rental.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
