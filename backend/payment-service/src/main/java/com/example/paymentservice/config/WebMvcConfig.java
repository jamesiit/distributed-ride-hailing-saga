package com.example.paymentservice.config;

import com.example.paymentservice.IdempotencyHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private IdempotencyHandler idempotencyHandler;

    public WebMvcConfig(IdempotencyHandler idempotencyHandler) {
        this.idempotencyHandler = idempotencyHandler;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(idempotencyHandler);
        WebMvcConfigurer.super.addInterceptors(registry);
    }
}
