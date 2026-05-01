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

import com.example.employeemanagement.dto.response.AuthResponse;
import com.example.employeemanagement.filter.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
    void shouldCreateEmployee() throws Exception {
        String token = authenticate("hr", "Hr@123");
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
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
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
        String token = authenticate("hr", "Hr@123");
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
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldRejectDuplicateEmployeeEmail() throws Exception {
        String token = authenticate("hr", "Hr@123");
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
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Employee email already exists"));
    }

    @Test
    void shouldRejectMissingDepartment() throws Exception {
        String token = authenticate("hr", "Hr@123");
        Map<String, Object> request = Map.of(
                "firstName", "Sara",
                "lastName", "Nabil",
                "email", "sara.nabil@example.com",
                "hireDate", "2024-05-01",
                "salary", 8000,
                "departmentId", 9999
        );

        mockMvc.perform(post("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Department not found with id: 9999"));
    }

    @Test
    void shouldReturnValidationErrorStructureWhenEmployeeRequestIsInvalid() throws Exception {
        String token = authenticate("hr", "Hr@123");
        Map<String, Object> request = Map.of(
                "firstName", "",
                "lastName", "Hassan",
                "email", "invalid-email",
                "hireDate", "2099-01-01",
                "salary", 0,
                "departmentId", getDepartmentIdByCode("ENG")
        );

        mockMvc.perform(post("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists(CorrelationIdFilter.HEADER_NAME))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value("Invalid request data"))
                .andExpect(jsonPath("$.path").value("/api/v1/employees"))
                .andExpect(jsonPath("$.correlationId").isString())
                .andExpect(jsonPath("$.validationErrors.firstName").value("First name is required"))
                .andExpect(jsonPath("$.validationErrors.email").value("Email must be valid"))
                .andExpect(jsonPath("$.validationErrors.salary").value("Salary must be greater than 0"));
    }

    @Test
    void shouldReturnUnauthorizedForEmployeeRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));
    }

    @Test
    void shouldReturnForbiddenWhenViewerCreatesEmployee() throws Exception {
        String token = authenticate("viewer", "Viewer@123");
        Map<String, Object> request = Map.of(
                "firstName", "Omar",
                "lastName", "Hassan",
                "email", "viewer.blocked@example.com",
                "hireDate", "2024-01-10",
                "salary", 12000,
                "departmentId", getDepartmentIdByCode("ENG")
        );

        mockMvc.perform(post("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }

    @Test
    void shouldBulkCreateEmployeesWithPartialSuccess() throws Exception {
        String token = authenticate("hr", "Hr@123");
        insertEmployee("Existing", "User", "existing@example.com", "ACTIVE", "Engineer", "ENG",
                LocalDate.of(2024, 1, 10), BigDecimal.valueOf(12000));

        List<Map<String, Object>> request = List.of(
                Map.of(
                        "firstName", "Omar",
                        "lastName", "Hany",
                        "email", "omar.bulk@example.com",
                        "phoneNumber", "+201000000000",
                        "hireDate", "2024-01-01",
                        "salary", 15000,
                        "status", "ACTIVE",
                        "jobTitle", "Backend Developer",
                        "departmentId", getDepartmentIdByCode("ENG")
                ),
                Map.of(
                        "firstName", "Existing",
                        "lastName", "User",
                        "email", "existing@example.com",
                        "hireDate", "2024-02-01",
                        "salary", 13000,
                        "departmentId", getDepartmentIdByCode("HR")
                ),
                Map.of(
                        "firstName", "No",
                        "lastName", "Department",
                        "email", "missing.department@example.com",
                        "hireDate", "2024-03-01",
                        "salary", 11000,
                        "departmentId", 9999
                ),
                Map.of(
                        "firstName", "Future",
                        "lastName", "Hire",
                        "email", "future.hire@example.com",
                        "hireDate", "2099-01-01",
                        "salary", 9000,
                        "departmentId", getDepartmentIdByCode("FIN")
                )
        );

        mockMvc.perform(post("/api/v1/employees/bulk")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.HEADER_NAME))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(3))
                .andExpect(jsonPath("$.errors", hasSize(3)))
                .andExpect(jsonPath("$.errors[0].identifier").value("existing@example.com"))
                .andExpect(jsonPath("$.errors[0].reason").value("Employee email already exists"))
                .andExpect(jsonPath("$.errors[1].identifier").value("missing.department@example.com"))
                .andExpect(jsonPath("$.errors[1].reason").value("Department not found with id: 9999"))
                .andExpect(jsonPath("$.errors[2].identifier").value("future.hire@example.com"))
                .andExpect(jsonPath("$.errors[2].reason").value("Hire date must be in the past or present"));

        mockMvc.perform(get("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .param("keyword", "omar.bulk@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("omar.bulk@example.com"));
    }

    @Test
    void shouldReturnEmployeesWithPaginationSortingAndDepartment() throws Exception {
        String token = authenticate("viewer", "Viewer@123");
        insertEmployee("Omar", "Hassan", "omar@example.com", "ACTIVE", "Engineer", "ENG",
                LocalDate.of(2024, 1, 10), BigDecimal.valueOf(12000));
        insertEmployee("Laila", "Fathy", "laila@example.com", "INACTIVE", "HR Specialist", "HR",
                LocalDate.of(2023, 7, 20), BigDecimal.valueOf(8000));
        insertEmployee("Youssef", "Adel", "youssef@example.com", "ACTIVE", "Accountant", "FIN",
                LocalDate.of(2024, 5, 5), BigDecimal.valueOf(9000));

        mockMvc.perform(get("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
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
        String token = authenticate("viewer", "Viewer@123");
        insertEmployee("Omar", "Hassan", "omar@example.com", "ACTIVE", "Backend Engineer", "ENG",
                LocalDate.of(2024, 1, 10), BigDecimal.valueOf(12000));
        insertEmployee("Laila", "Fathy", "laila@example.com", "INACTIVE", "HR Specialist", "HR",
                LocalDate.of(2023, 7, 20), BigDecimal.valueOf(8000));
        insertEmployee("Omar", "Saeed", "os@example.com", "ACTIVE", "Finance Analyst", "FIN",
                LocalDate.of(2024, 5, 5), BigDecimal.valueOf(14000));

        mockMvc.perform(get("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
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
        String token = authenticate("viewer", "Viewer@123");
        long employeeId = insertEmployee("Omar", "Hassan", "omar@example.com", "ACTIVE", "Backend Engineer", "ENG",
                LocalDate.of(2024, 1, 10), BigDecimal.valueOf(12000));

        mockMvc.perform(get("/api/v1/employees/{id}", employeeId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employeeId))
                .andExpect(jsonPath("$.department.code").value("ENG"));
    }

    @Test
    void shouldUpdateEmployee() throws Exception {
        String token = authenticate("hr", "Hr@123");
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
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("omar.updated@example.com"))
                .andExpect(jsonPath("$.status").value("ON_LEAVE"))
                .andExpect(jsonPath("$.department.code").value("OPS"));
    }

    @Test
    void shouldChangeEmployeeStatus() throws Exception {
        String token = authenticate("hr", "Hr@123");
        long employeeId = insertEmployee("Omar", "Hassan", "omar@example.com", "ACTIVE", "Backend Engineer", "ENG",
                LocalDate.of(2024, 1, 10), BigDecimal.valueOf(12000));

        mockMvc.perform(patch("/api/v1/employees/{id}/status", employeeId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ON_LEAVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ON_LEAVE"));
    }

    @Test
    void shouldBulkUpdateEmployeeStatusWithPartialSuccess() throws Exception {
        String token = authenticate("hr", "Hr@123");
        long activeEmployeeId = insertEmployee("Omar", "Hassan", "omar@example.com", "ACTIVE", "Backend Engineer", "ENG",
                LocalDate.of(2024, 1, 10), BigDecimal.valueOf(12000));
        long onLeaveEmployeeId = insertEmployee("Laila", "Fathy", "laila@example.com", "ON_LEAVE", "HR Specialist", "HR",
                LocalDate.of(2023, 7, 20), BigDecimal.valueOf(8000));
        long terminatedEmployeeId = insertEmployee("Mina", "Nader", "mina@example.com", "TERMINATED", "Analyst", "FIN",
                LocalDate.of(2022, 5, 15), BigDecimal.valueOf(9500));

        Map<String, Object> request = Map.of(
                "employeeIds", List.of(activeEmployeeId, onLeaveEmployeeId, 9999L, terminatedEmployeeId),
                "status", "ACTIVE"
        );

        mockMvc.perform(patch("/api/v1/employees/bulk/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.HEADER_NAME))
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.failedCount").value(2))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[0].identifier").value("9999"))
                .andExpect(jsonPath("$.errors[0].reason").value("Employee not found with id: 9999"))
                .andExpect(jsonPath("$.errors[1].identifier").value(String.valueOf(terminatedEmployeeId)))
                .andExpect(jsonPath("$.errors[1].reason").value("Terminated employee cannot be reactivated"));

        mockMvc.perform(get("/api/v1/employees/{id}", activeEmployeeId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/employees/{id}", onLeaveEmployeeId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/employees/{id}", terminatedEmployeeId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"));
    }

    @Test
    void shouldBlockReactivatingTerminatedEmployee() throws Exception {
        String token = authenticate("hr", "Hr@123");
        long employeeId = insertEmployee("Omar", "Hassan", "omar@example.com", "TERMINATED", "Backend Engineer", "ENG",
                LocalDate.of(2024, 1, 10), BigDecimal.valueOf(12000));

        mockMvc.perform(patch("/api/v1/employees/{id}/status", employeeId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ACTIVE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Terminated employee cannot be reactivated"));
    }

    @Test
    void shouldSoftDeleteEmployee() throws Exception {
        String token = authenticate("hr", "Hr@123");
        long employeeId = insertEmployee("Omar", "Hassan", "omar@example.com", "ACTIVE", "Backend Engineer", "ENG",
                LocalDate.of(2024, 1, 10), BigDecimal.valueOf(12000));

        mockMvc.perform(delete("/api/v1/employees/{id}", employeeId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/employees/{id}", employeeId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"));
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

}
