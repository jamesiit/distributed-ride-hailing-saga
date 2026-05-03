package com.example.paymentservice;

import com.example.paymentservice.model.IdempotentKey;
import com.example.paymentservice.model.IdempotentKeyStatus;
import com.example.paymentservice.service.IdempotentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.io.PrintWriter;
import java.util.UUID;

@Component
public class IdempotencyHandler implements HandlerInterceptor {

    private IdempotentService idempotentService;

    public IdempotencyHandler( IdempotentService idempotentService) {
        this.idempotentService = idempotentService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String recIdempotentKey = request.getHeader("Idempotency-Key");

        System.out.println(recIdempotentKey);

        try {
            IdempotentKeyStatus status = idempotentService.processRequest(recIdempotentKey);

            if (status == IdempotentKeyStatus.PROCEED) {
                System.out.println("Proceeding to controller...");
                return true;
            }

            if (status == IdempotentKeyStatus.CACHED) {
                IdempotentKey cachedKey = idempotentService.makeCache(recIdempotentKey);

                // we be dissecting the physical internet!

                if (cachedKey == null) {
                    // if cachedKey could not be found

                    response.setStatus(HttpServletResponse.SC_CONFLICT);

                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");

                    PrintWriter writer = response.getWriter();
                    String cacheError = """
                            {
                                "error": "Could not cache!"
                            }
                            """;
                    writer.write(cacheError);
                    writer.flush();

                    return false;

                }

                // return the key if it has been found

                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                String convCache = cachedKey.toString();

                PrintWriter writer = response.getWriter();

                String formatCache = """
                            {
                                "state": "Cached",
                                "transactionId": "%s"
                            }
                            """.formatted(convCache);

                writer.write(formatCache);
                writer.flush();

                return false;
            }

            if (status == IdempotentKeyStatus.PROCESSING) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                PrintWriter writer = response.getWriter();

                String processState = """
                            {
                                "state": "Processing",
                                "message": "Key is being processed!"
                            }
                            """;

                writer.write(processState);
                writer.flush();

                return false;
            }


        } catch (Exception e) {

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            PrintWriter writer = response.getWriter();

            String errorBody = """
                            {
                                "error": "Something went wrong!",
                            }
                            """;

            writer.write(errorBody);
            writer.flush();

            return false;
        }

        return false;

    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        System.out.println("postHandle() called");
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {

            String responseKey = request.getHeader("Idempotency-Key");

            UUID convKey = UUID.fromString(responseKey);

            if (response.getStatus() == HttpServletResponse.SC_CREATED) {
                idempotentService.updateCreatedStatus(convKey);
            }

    }
}
