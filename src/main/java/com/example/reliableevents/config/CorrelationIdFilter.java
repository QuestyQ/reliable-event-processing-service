package com.example.reliableevents.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = headerOrRandom(request, REQUEST_ID_HEADER);
        String correlationId = headerOrRandom(request, CORRELATION_HEADER);

        MDC.put("requestId", requestId);
        MDC.put("correlationId", correlationId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader(CORRELATION_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
            MDC.remove("correlationId");
        }
    }

    private String headerOrRandom(HttpServletRequest request, String header) {
        String value = request.getHeader(header);
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return value;
    }
}
