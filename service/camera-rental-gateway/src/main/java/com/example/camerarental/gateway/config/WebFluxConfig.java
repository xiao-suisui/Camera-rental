package com.example.camerarental.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

/**
 * WebFlux 配置类
 * 用于处理 favicon.ico 等静态资源请求
 */
@Configuration
public class WebFluxConfig {

    /**
     * 处理 favicon.ico 请求
     * 返回空的 ICO 文件以避免 404 错误
     */
    @Bean
    public RouterFunction<ServerResponse> faviconRoute() {
        // 创建一个简单的 16x16 透明 ICO 文件
        byte[] emptyIco = createEmptyIco();
        Resource faviconResource = new ByteArrayResource(emptyIco);

        return route(GET("/static/favicon.ico"),
            request -> ok()
                .contentType(MediaType.parseMediaType("image/x-icon"))
                .bodyValue(faviconResource));
    }

    /**
     * 创建空的 ICO 文件
     * 这是一个最小的有效 ICO 文件头
     */
    private byte[] createEmptyIco() {
        // 简单的 ICO 文件头 (22 bytes)
        return new byte[] {
            0x00, 0x00,  // Reserved
            0x01, 0x00,  // Icon type
            0x01, 0x00,  // Number of images
            0x10, 0x10,  // Width and height (16x16)
            0x00,       // Color palette
            0x00,       // Reserved
            0x01, 0x00,  // Color planes
            0x20, 0x00,  // Bits per pixel
            (byte) 0x88, 0x00, 0x00, 0x00,  // Image size (136 bytes)
            0x16, 0x00, 0x00, 0x00   // Image offset
        };
    }
}
