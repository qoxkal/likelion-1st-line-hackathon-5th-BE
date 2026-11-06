package com.line1.booktalk.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 프론트(Netlify, 로컬 개발 서버)에서 API를 호출할 수 있게 허용한다.
 * 허용 주소는 application.yml의 app.cors.allowed-origins에서 관리한다.
 * (나중에 Spring Security를 추가하면 Security 설정에서도 cors를 켜야 한다.)
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Request-Id")
                .maxAge(3600);
    }
}
