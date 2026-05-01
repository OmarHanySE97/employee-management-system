package com.example.employeemanagement.security;

import com.example.employeemanagement.exception.ErrorResponse;
import com.example.employeemanagement.filter.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Writes the standardized JSON response for unauthenticated access attempts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Returns a JSON 401 response for requests that require authentication.
     *
     * @param request the current HTTP request
     * @param response the current HTTP response
     * @param authException the authentication failure
     * @throws IOException if the response body cannot be written
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        String correlationId = resolveCorrelationId(request);
        log.warn(
                "Unauthorized request rejected: method={}, path={}, correlationId={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                correlationId,
                authException.getMessage()
        );

        if (correlationId != null) {
            response.setHeader(CorrelationIdFilter.HEADER_NAME, correlationId);
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Authentication Failed")
                .message("Authentication is required to access this resource")
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    /**
     * Resolves the correlation identifier captured earlier in the request lifecycle.
     *
     * @param request the current HTTP request
     * @return the correlation identifier when available
     */
    private String resolveCorrelationId(HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        if (correlationId instanceof String value && !value.isBlank()) {
            return value;
        }
        return null;
    }
}
