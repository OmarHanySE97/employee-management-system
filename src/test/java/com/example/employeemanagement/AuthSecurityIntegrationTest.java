package com.example.employeemanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.employeemanagement.dto.response.AuthResponse;
import com.example.employeemanagement.filter.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("employee_management")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE employees, departments, users RESTART IDENTITY CASCADE");
        jdbcTemplate.update("""
                INSERT INTO departments (name, code, description, created_at, updated_at)
                VALUES
                    ('Engineering', 'ENG', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('Human Resources', 'HR', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('Finance', 'FIN', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('Operations', 'OPS', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO users (username, password, role, created_at, updated_at)
                VALUES
                    ('admin', '$2a$10$GExjezD1njQgRoArr/PZN.LUwdbPcakHVypvfhrHOIV0MWVOifj7G', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('hr', '$2a$10$JISk9j5l4rCqFgi9fryIZOTgYbuTD8Bj.vzmM3sepBb/9wmtTq5ba', 'HR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('viewer', '$2a$10$ZBGEWvR08r8FvNnsi2XMcuVZ03xukv0o9dJKDXPu8X58sVB5mAKpi', 'VIEWER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
    }

    @Test
    void shouldRegisterUserAndReturnToken() throws Exception {
        Map<String, Object> request = Map.of(
                "username", "security.hr",
                "password", "Admin@123",
                "role", "HR"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(CorrelationIdFilter.HEADER_NAME))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                "security.hr"
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldLoginAndReturnToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "admin",
                                "password", "Admin@123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.HEADER_NAME))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void shouldReturnUnauthorizedForProtectedRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));
    }

    @Test
    void shouldReturnForbiddenForInsufficientRole() throws Exception {
        String token = authenticate("viewer", "Viewer@123");

        mockMvc.perform(post("/api/v1/departments")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Legal",
                                "code", "LGL"
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }

    @Test
    void shouldAllowProtectedRequestWithValidToken() throws Exception {
        String token = authenticate("hr", "Hr@123");

        mockMvc.perform(get("/api/v1/departments")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token)))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.HEADER_NAME))
                .andExpect(jsonPath("$.content").isArray());
    }

    private String authenticate(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                AuthResponse.class
        );
        return response.getAccessToken();
    }

    private String bearerToken(String token) {
        return "Bearer " + token;
    }
}
