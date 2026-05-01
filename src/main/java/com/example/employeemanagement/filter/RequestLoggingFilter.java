package com.example.employeemanagement.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Logs a lightweight summary for each HTTP request without exposing sensitive data.
 *
 * <p>The log entry includes the request method, path, response status, duration, and
 * correlation identifier. It intentionally avoids logging headers, request bodies,
 * passwords, authorization values, and JWT tokens.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    /**
     * Wraps request processing and logs the final response status with execution time.
     *
     * @param request the current HTTP request
     * @param response the current HTTP response
     * @param filterChain the remaining filter chain
     * @throws ServletException if request processing fails at the servlet layer
     * @throws IOException if request or response I/O fails
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startTime = System.nanoTime();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMillis = (System.nanoTime() - startTime) / 1_000_000;
            Object correlationId = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);

            log.info(
                    "HTTP request completed: method={}, path={}, status={}, durationMs={}, correlationId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMillis,
                    correlationId
            );
        }
    }
}
