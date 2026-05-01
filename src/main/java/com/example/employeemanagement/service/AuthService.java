package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.request.LoginRequest;
import com.example.employeemanagement.dto.request.RegisterRequest;
import com.example.employeemanagement.dto.response.AuthResponse;

/**
 * Defines authentication and registration use cases.
 */
public interface AuthService {

    /**
     * Registers a new user account.
     *
     * @param request the registration payload
     * @return the authentication response for the created user
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates a user with username and password.
     *
     * @param request the login payload
     * @return the authentication response containing a JWT
     */
    AuthResponse login(LoginRequest request);
}
