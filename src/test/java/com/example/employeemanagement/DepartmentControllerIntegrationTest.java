package com.example.employeemanagement;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.employeemanagement.filter.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@Import(DepartmentControllerIntegrationTest.TestSecurityConfig.class)
class DepartmentControllerIntegrationTest {

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
        jdbcTemplate.execute("TRUNCATE TABLE employees, departments RESTART IDENTITY CASCADE");
        jdbcTemplate.update("""
                INSERT INTO departments (name, code, description, created_at, updated_at)
                VALUES
                    ('Engineering', 'ENG', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('Human Resources', 'HR', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('Finance', 'FIN', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('Operations', 'OPS', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
    }

    @Test
    void shouldCreateDepartment() throws Exception {
        Map<String, Object> request = Map.of(
                "name", "Product",
                "code", "PRD",
                "description", "Product department"
        );

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(CorrelationIdFilter.HEADER_NAME))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Product"))
                .andExpect(jsonPath("$.code").value("PRD"))
                .andExpect(jsonPath("$.description").value("Product department"));
    }

    @Test
    void shouldRejectDuplicateDepartmentName() throws Exception {
        Map<String, Object> request = Map.of(
                "name", "Engineering",
                "code", "PRD"
        );

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Department name already exists"));
    }

    @Test
    void shouldReturnPaginatedDepartments() throws Exception {
        mockMvc.perform(get("/api/v1/departments")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].name").value("Engineering"));
    }

    @Test
    void shouldReturnDepartmentById() throws Exception {
        Long departmentId = getDepartmentIdByCode("ENG");

        mockMvc.perform(get("/api/v1/departments/{id}", departmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(departmentId))
                .andExpect(jsonPath("$.name").value("Engineering"))
                .andExpect(jsonPath("$.code").value("ENG"));
    }

    @Test
    void shouldReturnNotFoundForMissingDepartment() throws Exception {
        mockMvc.perform(get("/api/v1/departments/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Department not found with id: 9999"));
    }

    @Test
    void shouldUpdateDepartment() throws Exception {
        Long departmentId = getDepartmentIdByCode("ENG");
        Map<String, Object> request = Map.of(
                "name", "Engineering and Platform",
                "code", "ENP",
                "description", "Updated description"
        );

        mockMvc.perform(put("/api/v1/departments/{id}", departmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(departmentId))
                .andExpect(jsonPath("$.name").value("Engineering and Platform"))
                .andExpect(jsonPath("$.code").value("ENP"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void shouldBlockDepartmentDeletionWhenEmployeesExist() throws Exception {
        Long departmentId = getDepartmentIdByCode("ENG");
        jdbcTemplate.update("""
                INSERT INTO employees (
                    first_name, last_name, email, phone_number, hire_date, salary, status, job_title,
                    department_id, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                "John",
                "Doe",
                "john.doe@example.com",
                "1234567890",
                LocalDate.now(),
                BigDecimal.valueOf(5000),
                "ACTIVE",
                "Engineer",
                departmentId
        );

        mockMvc.perform(delete("/api/v1/departments/{id}", departmentId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Department cannot be deleted because employees are assigned to it"));
    }

    @Test
    void shouldDeleteDepartmentWhenNoEmployeesExist() throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Legal",
                                "code", "LGL"
                        ))))
                .andExpect(status().isCreated());

        Long departmentId = getDepartmentIdByCode("LGL");

        mockMvc.perform(delete("/api/v1/departments/{id}", departmentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/departments/{id}", departmentId))
                .andExpect(status().isNotFound());
    }

    private Long getDepartmentIdByCode(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM departments WHERE code = ?",
                Long.class,
                code
        );
    }

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        @Order(0)
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .securityMatcher("/api/v1/departments/**")
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authorize -> authorize
                            .anyRequest().permitAll()
                    )
                    .anonymous(anonymous -> anonymous
                            .principal("test-admin")
                            .authorities("ROLE_ADMIN")
                    )
                    .build();
        }
    }
}
