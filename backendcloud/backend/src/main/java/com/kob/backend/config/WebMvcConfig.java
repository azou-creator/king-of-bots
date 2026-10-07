package com.kob.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 用户上传文件的静态访问映射:
 *   GET /avatar/**  ->  本地 avatar.dir 目录
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${avatar.dir}")
    private String avatarDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = Paths.get(avatarDir).toAbsolutePath().normalize().toString();
        registry.addResourceHandler("/avatar/**")
                .addResourceLocations("file:" + dir + "/");
    }
}
