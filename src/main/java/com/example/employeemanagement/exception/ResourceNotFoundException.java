package com.example.employeemanagement.exception;

/**
 * Represents a missing resource lookup failure.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Creates a new resource not found exception with a message.
     *
     * @param message the not-found message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a new resource not found exception with a message and cause.
     *
     * @param message the not-found message
     * @param cause the underlying cause
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
