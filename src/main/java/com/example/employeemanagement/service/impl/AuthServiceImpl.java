package com.example.employeemanagement.service.impl;

import com.example.employeemanagement.dto.request.LoginRequest;
import com.example.employeemanagement.dto.request.RegisterRequest;
import com.example.employeemanagement.dto.response.AuthResponse;
import com.example.employeemanagement.entity.User;
import com.example.employeemanagement.exception.DuplicateResourceException;
import com.example.employeemanagement.repository.UserRepository;
import com.example.employeemanagement.security.JwtService;
import com.example.employeemanagement.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of authentication and registration use cases.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Registers a new user after ensuring the username is unique.
     *
     * @param request the registration payload
     * @return the authentication response for the created user
     */
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = normalizeRequiredValue(request.getUsername());

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: username={}, role={}",
                savedUser.getUsername(),
                savedUser.getRole());

        String token = jwtService.generateToken(buildUserDetails(savedUser));
        return new AuthResponse(token, TOKEN_TYPE);
    }

    /**
     * Authenticates a user and returns a JWT access token.
     *
     * @param request the login payload
     * @return the authentication response for the authenticated user
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        String username = normalizeRequiredValue(request.getUsername());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        log.info("User authenticated successfully: username={}", userDetails.getUsername());

        return new AuthResponse(token, TOKEN_TYPE);
    }

    /**
     * Adapts the persisted user entity to Spring Security's {@link UserDetails} model.
     *
     * @param user the persisted user entity
     * @return the corresponding user details instance
     */
    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }

    /**
     * Trims a required string value before persistence or lookup.
     *
     * @param value the raw input value
     * @return the trimmed value
     */
    private String normalizeRequiredValue(String value) {
        return value.trim();
    }
}
