package com.example.employeemanagement.exception;

/**
 * Represents a business rule violation that should be returned as a client-safe error.
 */
public class BusinessException extends RuntimeException {

    /**
     * Creates a new business exception with a message.
     *
     * @param message the business error message
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * Creates a new business exception with a message and cause.
     *
     * @param message the business error message
     * @param cause the underlying cause
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
