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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Writes the standardized JSON response for authenticated users lacking required permissions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * Returns a JSON 403 response for forbidden requests.
     *
     * @param request the current HTTP request
     * @param response the current HTTP response
     * @param accessDeniedException the authorization failure
     * @throws IOException if the response body cannot be written
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        String correlationId = resolveCorrelationId(request);
        log.warn(
                "Forbidden request rejected: method={}, path={}, correlationId={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                correlationId,
                accessDeniedException.getMessage()
        );

        if (correlationId != null) {
            response.setHeader(CorrelationIdFilter.HEADER_NAME, correlationId);
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
                .message("You do not have permission to access this resource")
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        response.setStatus(HttpStatus.FORBIDDEN.value());
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
