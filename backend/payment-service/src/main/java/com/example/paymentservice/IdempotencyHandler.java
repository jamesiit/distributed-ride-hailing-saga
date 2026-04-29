package com.example.paymentservice;

import com.example.paymentservice.model.IdempotentKeyStatus;
import com.example.paymentservice.service.IdempotentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class IdempotencyHandler implements HandlerInterceptor {

    private IdempotentService idempotentService;

    public IdempotencyHandler( IdempotentService idempotentService) {
        this.idempotentService = idempotentService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String recIdempotentKey = request.getHeader("Idempotency-Key");

        try {
            IdempotentKeyStatus status = idempotentService.processRequest(recIdempotentKey);

            if (status == IdempotentKeyStatus.PROCEED) {
                return true;
            }


        } catch (Exception e) {
            System.out.println("Oh no! There's an Exception!");
            return false;
        }

    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        System.out.println("postHandle() called");
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        System.out.println("afterCompletion() called");
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
