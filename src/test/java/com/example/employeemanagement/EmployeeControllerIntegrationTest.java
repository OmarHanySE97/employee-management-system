package com.example.employeemanagement;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@Import(EmployeeControllerIntegrationTest.TestSecurityConfig.class)
class EmployeeControllerIntegrationTest {

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
    void shouldCreateEmployee() throws Exception {
        Map<String, Object> request = Map.of(
                "firstName", "Omar",
                "lastName", "Hassan",
                "email", "omar.hassan@example.com",
                "phoneNumber", "01000000000",
                "hireDate", "2024-01-10",
                "salary", 12000,
                "status", "ACTIVE",
                "jobTitle", "Backend Engineer",
                "departmentId", getDepartmentIdByCode("ENG")
        );

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(CorrelationIdFilter.HEADER_NAME))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("omar.hassan@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.department.code").value("ENG"));
    }

    @Test
    void shouldDefaultStatusToActiveWhenMissing() throws Exception {
        Map<String, Object> request = Map.of(
                "firstName", "Mona",
                "lastName", "Ali",
                "email", "mona.ali@example.com",
                "hireDate", "2024-03-15",
                "salary", 9500,
                "jobTitle", "Analyst",
                "departmentId", getDepartmentIdByCode("FIN")
        );

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldRejectDuplicateEmployeeEmail() throws Exception {
        insertEmployee("Omar", "Hassan", "omar.hassan@example.com", "ACTIVE", "Backend Engineer", "ENG",
                LocalDate.of(2024, 1, 10), BigDecimal.valueOf(12000));

        Map<String, Object> request = Map.of(
                "firstName", "Another",
                "lastName", "User",
                "email", "omar.hassan@example.com",
                "hireDate", "2024-05-01",
                "salary", 11000,
                "departmentId", getDepartmentIdByCode("ENG")
        );

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Employee email already exists"));
    }

    @Test
    void shouldRejectMissingDepartment() throws Exception {
        Map<String, Object> request = Map.of(
                "firstName", "Sara",
                "lastName", "Nabil",
                "email", "sara.nabil@example.com",
                "hireDate", "2024-05-01",
                "salary", 8000,
                "departmentId", 9999
        );

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Department not found with id: 9999"));
    }

    @Test
    void shouldReturnEmployeesWithPaginationSortingAndDepartment() throws Exception {
        insertEmployee("Omar", "Hassan", "omar@example.com", "ACTIVE", "Engineer", "ENG",
                LocalDate.of(2024, 1, 10), BigDecimal.valueOf(12000));
        insertEmployee("Laila", "Fathy", "laila@example.com", "INACTIVE", "HR Specialist", "HR",
                LocalDate.of(2023, 7, 20), BigDecimal.valueOf(8000));
        insertEmployee("Youssef", "Adel", "youssef@example.com", "ACTIVE", "Accountant", "FIN",
                LocalDate.of(2024, 5, 5), BigDecimal.valueOf(9000));

        mockMvc.perform(get("/api/v1/employees")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "hireDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].email").value("youssef@example.com"))
                .andExpect(jsonPath("$.content[0].department.code").value("FIN"));
    }

    @Test
    void shouldFilterEmployeesByStatusDepartmentKeywordSalaryAndHireDate() throws Exception {
        insertEmployee("Omar", "Hassan", "omar@example.com", "ACTIVE", "Backend Engineer", "ENG",
                LocalDate.of(2024, 1, 10), BigDecimal.valueOf(12000));
        insertEmployee("Laila", "Fathy", "laila@example.com", "INACTIVE", "HR Specialist", "HR",
                LocalDate.of(2023, 7, 20), BigDecimal.valueOf(8000));
        insertEmployee("Omar", "Saeed", "os@example.com", "ACTIVE", "Finance Analyst", "FIN",
                LocalDate.of(2024, 5, 5), BigDecimal.valueOf(14000));

        mockMvc.perform(get("/api/v1/employees")
                        .param("status", "ACTIVE")
                        .param("departmentId", String.valueOf(getDepartmentIdByCode("FIN")))
                        .param("keyword", "omar")
                        .param("minSalary", "10000")
                        .param("maxSalary", "15000")
                        .param("hireDateFrom", "2024-01-01")
                        .param("hireDateTo", "2025-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email").value("os@example.com"))
                .andExpect(jsonPath("$.content[0].department.code").value("FIN"));
    }

    @Test
    void shouldReturnEmployeeById() throws Exception {
        long employeeId = insertEmployee("Omar", "Hassan", "omar@example.com", "ACTIVE", "Backend Engineer", "ENG",
                LocalDate.of(2024, 1, 10), BigDecimal.valueOf(12000));

        mockMvc.perform(get("/api/v1/employees/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employeeId))
                .andExpect(jsonPath("$.department.code").value("ENG"));
    }

    @Test
    void shouldUpdateEmployee() throws Exception {
        long employeeId = insertEmployee("Omar", "Hassan", "omar@example.com", "ACTIVE", "Backend Engineer", "ENG",
                LocalDate.of(2024, 1, 10), BigDecimal.valueOf(12000));

        Map<String, Object> request = Map.of(
                "firstName", "Omar",
                "lastName", "Hassan Updated",
                "email", "omar.updated@example.com",
                "phoneNumber", "01111111111",
                "hireDate", "2024-01-10",
                "salary", 15000,
                "status", "ON_LEAVE",
                "jobTitle", "Lead Engineer",
                "departmentId", getDepartmentIdByCode("OPS")
        );

        mockMvc.perform(put("/api/v1/employees/{id}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("omar.updated@example.com"))
                .andExpect(jsonPath("$.status").value("ON_LEAVE"))
                .andExpect(jsonPath("$.department.code").value("OPS"));
    }

    @Test
    void shouldChangeEmployeeStatus() throws Exception {
        long employeeId = insertEmployee("Omar", "Hassan", "omar@example.com", "ACTIVE", "Backend Engineer", "ENG",
                LocalDate.of(2024, 1, 10), BigDecimal.valueOf(12000));

        mockMvc.perform(patch("/api/v1/employees/{id}/status", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ON_LEAVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ON_LEAVE"));
    }

    @Test
    void shouldBlockReactivatingTerminatedEmployee() throws Exception {
        long employeeId = insertEmployee("Omar", "Hassan", "omar@example.com", "TERMINATED", "Backend Engineer", "ENG",
                LocalDate.of(2024, 1, 10), BigDecimal.valueOf(12000));

        mockMvc.perform(patch("/api/v1/employees/{id}/status", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ACTIVE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Terminated employee cannot be reactivated"));
    }

    @Test
    void shouldSoftDeleteEmployee() throws Exception {
        long employeeId = insertEmployee("Omar", "Hassan", "omar@example.com", "ACTIVE", "Backend Engineer", "ENG",
                LocalDate.of(2024, 1, 10), BigDecimal.valueOf(12000));

        mockMvc.perform(delete("/api/v1/employees/{id}", employeeId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/employees/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"));
    }

    private Long getDepartmentIdByCode(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM departments WHERE code = ?",
                Long.class,
                code
        );
    }

    private long insertEmployee(
            String firstName,
            String lastName,
            String email,
            String status,
            String jobTitle,
            String departmentCode,
            LocalDate hireDate,
            BigDecimal salary
    ) {
        jdbcTemplate.update("""
                INSERT INTO employees (
                    first_name, last_name, email, phone_number, hire_date, salary, status, job_title,
                    department_id, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                firstName,
                lastName,
                email,
                "01000000000",
                hireDate,
                salary,
                status,
                jobTitle,
                getDepartmentIdByCode(departmentCode)
        );

        return jdbcTemplate.queryForObject(
                "SELECT id FROM employees WHERE email = ?",
                Long.class,
                email
        );
    }

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/api/v1/employees/**").permitAll()
                            .anyRequest().authenticated()
                    )
                    .httpBasic(Customizer.withDefaults())
                    .build();
        }
    }
}
