package com.example.employeemanagement.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.employeemanagement.dto.request.LoginRequest;
import com.example.employeemanagement.dto.request.RegisterRequest;
import com.example.employeemanagement.dto.response.AuthResponse;
import com.example.employeemanagement.entity.User;
import com.example.employeemanagement.enums.Role;
import com.example.employeemanagement.exception.DuplicateResourceException;
import com.example.employeemanagement.repository.UserRepository;
import com.example.employeemanagement.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerShouldReturnTokenWhenUsernameIsAvailable() {
        RegisterRequest request = new RegisterRequest(" admin ", "Admin@123", Role.ADMIN);
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("admin");
        savedUser.setPassword("encoded-password");
        savedUser.setRole(Role.ADMIN);

        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("Admin@123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User userToSave = userCaptor.getValue();
        assertThat(userToSave.getUsername()).isEqualTo("admin");
        assertThat(userToSave.getPassword()).isEqualTo("encoded-password");
        assertThat(userToSave.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void registerShouldThrowWhenUsernameAlreadyExists() {
        RegisterRequest request = new RegisterRequest("admin", "Admin@123", Role.ADMIN);
        DuplicateResourceException exception = new DuplicateResourceException("Username already exists");

        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void loginShouldReturnTokenWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest(" admin ", "Admin@123");
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("admin")
                .password("encoded-password")
                .roles("ADMIN")
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authCaptor.capture());
        assertThat(authCaptor.getValue().getPrincipal()).isEqualTo("admin");
        assertThat(authCaptor.getValue().getCredentials()).isEqualTo("Admin@123");
    }

    @Test
    void loginShouldThrowWhenCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest("admin", "wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");

        verify(jwtService, never()).generateToken(any(UserDetails.class));
    }
}
