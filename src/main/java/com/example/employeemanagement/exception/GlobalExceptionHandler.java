package com.example.employeemanagement.exception;

import com.example.employeemanagement.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralizes API exception handling and converts failures into the shared error response format.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles bean validation failures raised for request bodies.
     *
     * @param exception the validation exception
     * @param request the current HTTP request
     * @return the standardized validation error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Validation failed for request body: method={}, path={}, correlationId={}, errorCount={}",
                request.getMethod(),
                request.getRequestURI(),
                resolveCorrelationId(request),
                exception.getBindingResult().getErrorCount()
        );

        Map<String, String> validationErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage()
                                : "Invalid value",
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "Invalid request data",
                request,
                validationErrors
        );
    }

    /**
     * Handles validation failures raised for request parameters and path variables.
     *
     * @param exception the constraint violation exception
     * @param request the current HTTP request
     * @return the standardized validation error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Constraint validation failed: method={}, path={}, correlationId={}, violationCount={}",
                request.getMethod(),
                request.getRequestURI(),
                resolveCorrelationId(request),
                exception.getConstraintViolations().size()
        );

        Map<String, String> validationErrors = exception.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> extractFieldName(violation),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "Invalid request data",
                request,
                validationErrors
        );
    }

    /**
     * Handles missing resource errors.
     *
     * @param exception the not-found exception
     * @param request the current HTTP request
     * @return the standardized not-found response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Resource not found: method={}, path={}, correlationId={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                resolveCorrelationId(request),
                exception.getMessage()
        );

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                exception.getMessage(),
                request
        );
    }

    /**
     * Handles duplicate resource conflicts.
     *
     * @param exception the duplicate resource exception
     * @param request the current HTTP request
     * @return the standardized conflict response
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Duplicate resource conflict: method={}, path={}, correlationId={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                resolveCorrelationId(request),
                exception.getMessage()
        );

        return buildResponse(
                HttpStatus.CONFLICT,
                "Duplicate Resource",
                exception.getMessage(),
                request
        );
    }

    /**
     * Handles domain business rule violations.
     *
     * @param exception the business exception
     * @param request the current HTTP request
     * @return the standardized business error response
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Business rule violation: method={}, path={}, correlationId={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                resolveCorrelationId(request),
                exception.getMessage()
        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Business Error",
                exception.getMessage(),
                request
        );
    }

    /**
     * Handles invalid username or password authentication failures.
     *
     * @param exception the bad credentials exception
     * @param request the current HTTP request
     * @return the standardized unauthorized response
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Authentication failed due to bad credentials: method={}, path={}, correlationId={}",
                request.getMethod(),
                request.getRequestURI(),
                resolveCorrelationId(request)
        );

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Authentication Failed",
                "Invalid username or password",
                request
        );
    }

    /**
     * Handles generic authentication failures.
     *
     * @param exception the authentication exception
     * @param request the current HTTP request
     * @return the standardized unauthorized response
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Authentication failed: method={}, path={}, correlationId={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                resolveCorrelationId(request),
                exception.getMessage()
        );

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Authentication Failed",
                exception.getMessage(),
                request
        );
    }

    /**
     * Handles authorization failures for authenticated users.
     *
     * @param exception the access denied exception
     * @param request the current HTTP request
     * @return the standardized forbidden response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Access denied: method={}, path={}, correlationId={}",
                request.getMethod(),
                request.getRequestURI(),
                resolveCorrelationId(request)
        );

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Access Denied",
                "You do not have permission to access this resource",
                request
        );
    }

    /**
     * Handles persistence conflicts raised by the database layer.
     *
     * @param exception the data integrity exception
     * @param request the current HTTP request
     * @return the standardized conflict response
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Data integrity conflict: method={}, path={}, correlationId={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                resolveCorrelationId(request),
                exception.getMostSpecificCause() != null
                        ? exception.getMostSpecificCause().getMessage()
                        : exception.getMessage()
        );

        return buildResponse(
                HttpStatus.CONFLICT,
                "Data Conflict",
                "The request could not be completed because it conflicts with existing data",
                request
        );
    }

    /**
     * Handles uncaught exceptions while logging server-side details for diagnosis.
     *
     * @param exception the unexpected exception
     * @param request the current HTTP request
     * @return the standardized internal server error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unhandled exception processed by global exception handler: method={}, path={}, correlationId={}",
                request.getMethod(),
                request.getRequestURI(),
                resolveCorrelationId(request),
                exception
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred",
                request
        );
    }

    /**
     * Builds a standardized error response without validation details.
     *
     * @param status the HTTP status to return
     * @param error the short error title
     * @param message the client-safe error message
     * @param request the current HTTP request
     * @return the HTTP response entity containing the error payload
     */
    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request
    ) {
        return buildResponse(status, error, message, request, null);
    }

    /**
     * Builds a standardized error response with optional validation details.
     *
     * @param status the HTTP status to return
     * @param error the short error title
     * @param message the client-safe error message
     * @param request the current HTTP request
     * @param validationErrors optional field-level validation errors
     * @return the HTTP response entity containing the error payload
     */
    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request,
            Map<String, String> validationErrors
    ) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .correlationId(resolveCorrelationId(request))
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Resolves the correlation identifier for the current request.
     *
     * @param request the current HTTP request
     * @return the correlation identifier when available
     */
    private String resolveCorrelationId(HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        if (correlationId instanceof String value && !value.isBlank()) {
            return value;
        }

        String headerCorrelationId = request.getHeader(CorrelationIdFilter.HEADER_NAME);
        if (headerCorrelationId != null && !headerCorrelationId.isBlank()) {
            request.setAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE, headerCorrelationId);
            return headerCorrelationId;
        }

        return null;
    }

    /**
     * Extracts the final field name segment from a constraint violation property path.
     *
     * @param violation the constraint violation to inspect
     * @return the field name used in the validation error response
     */
    private String extractFieldName(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        int lastDotIndex = propertyPath.lastIndexOf('.');
        if (lastDotIndex >= 0 && lastDotIndex < propertyPath.length() - 1) {
            return propertyPath.substring(lastDotIndex + 1);
        }
        return propertyPath;
    }
}
