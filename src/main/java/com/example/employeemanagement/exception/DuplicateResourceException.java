package com.example.employeemanagement.exception;

/**
 * Represents a duplicate resource conflict detected by business validation.
 */
public class DuplicateResourceException extends BusinessException {

    /**
     * Creates a new duplicate resource exception with a message.
     *
     * @param message the conflict message
     */
    public DuplicateResourceException(String message) {
        super(message);
    }

    /**
     * Creates a new duplicate resource exception with a message and cause.
     *
     * @param message the conflict message
     * @param cause the underlying cause
     */
    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
