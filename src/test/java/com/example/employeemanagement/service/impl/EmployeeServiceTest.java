package com.example.employeemanagement.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.employeemanagement.dto.request.BulkEmployeeStatusUpdateRequest;
import com.example.employeemanagement.dto.request.EmployeeCreateRequest;
import com.example.employeemanagement.dto.request.EmployeeStatusUpdateRequest;
import com.example.employeemanagement.dto.request.EmployeeUpdateRequest;
import com.example.employeemanagement.dto.response.BulkOperationResponse;
import com.example.employeemanagement.dto.response.EmployeeResponse;
import com.example.employeemanagement.entity.Department;
import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.enums.EmployeeStatus;
import com.example.employeemanagement.exception.BusinessException;
import com.example.employeemanagement.exception.DuplicateResourceException;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.mapper.EmployeeMapper;
import com.example.employeemanagement.repository.EmployeeRepository;
import com.example.employeemanagement.validation.EmployeeValidator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private EmployeeValidator employeeValidator;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Test
    void createEmployeeShouldReturnEmployeeResponseWhenRequestIsValid() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                " Omar ",
                " Hany ",
                " omar@example.com ",
                " 01000000000 ",
                LocalDate.of(2024, 1, 1),
                BigDecimal.valueOf(15000),
                EmployeeStatus.ACTIVE,
                " Backend Developer ",
                1L
        );
        Department department = createDepartment(1L, "Engineering", "ENG");
        EmployeeResponse expectedResponse = createEmployeeResponse(10L, "omar@example.com", EmployeeStatus.ACTIVE);

        when(employeeValidator.validateDepartmentExists(1L)).thenReturn(department);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(10L);
            return employee;
        });
        when(employeeMapper.toResponse(any(Employee.class))).thenReturn(expectedResponse);

        EmployeeResponse actualResponse = employeeService.createEmployee(request);

        assertThat(actualResponse).isSameAs(expectedResponse);
        verify(employeeValidator).validateEmailUniqueness("omar@example.com");

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(employeeCaptor.capture());
        Employee savedEmployee = employeeCaptor.getValue();
        assertThat(savedEmployee.getFirstName()).isEqualTo("Omar");
        assertThat(savedEmployee.getLastName()).isEqualTo("Hany");
        assertThat(savedEmployee.getEmail()).isEqualTo("omar@example.com");
        assertThat(savedEmployee.getPhoneNumber()).isEqualTo("01000000000");
        assertThat(savedEmployee.getJobTitle()).isEqualTo("Backend Developer");
        assertThat(savedEmployee.getDepartment()).isSameAs(department);
        assertThat(savedEmployee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    void createEmployeeShouldThrowWhenDepartmentDoesNotExist() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Omar",
                "Hany",
                "omar@example.com",
                null,
                LocalDate.of(2024, 1, 1),
                BigDecimal.valueOf(15000),
                EmployeeStatus.ACTIVE,
                null,
                99L
        );
        ResourceNotFoundException exception = new ResourceNotFoundException("Department not found with id: 99");

        when(employeeValidator.validateDepartmentExists(99L)).thenThrow(exception);

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isSameAs(exception);

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void createEmployeeShouldThrowWhenEmailAlreadyExists() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Omar",
                "Hany",
                "omar@example.com",
                null,
                LocalDate.of(2024, 1, 1),
                BigDecimal.valueOf(15000),
                EmployeeStatus.ACTIVE,
                null,
                1L
        );
        DuplicateResourceException exception = new DuplicateResourceException("Employee email already exists");

        doThrow(exception).when(employeeValidator).validateEmailUniqueness("omar@example.com");

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isSameAs(exception);

        verify(employeeValidator, never()).validateDepartmentExists(any(Long.class));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void updateEmployeeShouldReturnUpdatedResponseWhenRequestIsValid() {
        Employee existingEmployee = createEmployee(7L, "old@example.com", EmployeeStatus.ACTIVE);
        Department operationsDepartment = createDepartment(4L, "Operations", "OPS");
        EmployeeUpdateRequest request = new EmployeeUpdateRequest(
                " Omar ",
                " Hassan ",
                " omar.updated@example.com ",
                " 01111111111 ",
                LocalDate.of(2024, 1, 10),
                BigDecimal.valueOf(18000),
                EmployeeStatus.ON_LEAVE,
                " Lead Engineer ",
                4L
        );
        EmployeeResponse expectedResponse = createEmployeeResponse(7L, "omar.updated@example.com", EmployeeStatus.ON_LEAVE);

        when(employeeValidator.validateEmployeeExists(7L)).thenReturn(existingEmployee);
        when(employeeValidator.validateDepartmentExists(4L)).thenReturn(operationsDepartment);
        when(employeeRepository.save(existingEmployee)).thenReturn(existingEmployee);
        when(employeeMapper.toResponse(existingEmployee)).thenReturn(expectedResponse);

        EmployeeResponse actualResponse = employeeService.updateEmployee(7L, request);

        assertThat(actualResponse).isSameAs(expectedResponse);
        verify(employeeValidator).validateEmailUniqueness("omar.updated@example.com", 7L);
        assertThat(existingEmployee.getFirstName()).isEqualTo("Omar");
        assertThat(existingEmployee.getLastName()).isEqualTo("Hassan");
        assertThat(existingEmployee.getEmail()).isEqualTo("omar.updated@example.com");
        assertThat(existingEmployee.getPhoneNumber()).isEqualTo("01111111111");
        assertThat(existingEmployee.getJobTitle()).isEqualTo("Lead Engineer");
        assertThat(existingEmployee.getDepartment()).isSameAs(operationsDepartment);
        assertThat(existingEmployee.getStatus()).isEqualTo(EmployeeStatus.ON_LEAVE);
    }

    @Test
    void changeEmployeeStatusShouldReturnUpdatedResponseWhenTransitionIsValid() {
        Employee employee = createEmployee(5L, "omar@example.com", EmployeeStatus.ACTIVE);
        EmployeeStatusUpdateRequest request = new EmployeeStatusUpdateRequest(EmployeeStatus.ON_LEAVE);
        EmployeeResponse expectedResponse = createEmployeeResponse(5L, "omar@example.com", EmployeeStatus.ON_LEAVE);

        when(employeeValidator.validateEmployeeExists(5L)).thenReturn(employee);
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(employeeMapper.toResponse(employee)).thenReturn(expectedResponse);

        EmployeeResponse actualResponse = employeeService.changeEmployeeStatus(5L, request);

        assertThat(actualResponse).isSameAs(expectedResponse);
        verify(employeeValidator).validateStatusTransition(employee, EmployeeStatus.ON_LEAVE);
        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ON_LEAVE);
    }

    @Test
    void changeEmployeeStatusShouldThrowWhenReactivatingTerminatedEmployee() {
        Employee employee = createEmployee(5L, "omar@example.com", EmployeeStatus.TERMINATED);
        EmployeeStatusUpdateRequest request = new EmployeeStatusUpdateRequest(EmployeeStatus.ACTIVE);
        BusinessException exception = new BusinessException("Terminated employee cannot be reactivated");

        when(employeeValidator.validateEmployeeExists(5L)).thenReturn(employee);
        doThrow(exception).when(employeeValidator).validateStatusTransition(employee, EmployeeStatus.ACTIVE);

        assertThatThrownBy(() -> employeeService.changeEmployeeStatus(5L, request))
                .isSameAs(exception);

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void deleteEmployeeShouldSetStatusToTerminatedWhenEmployeeExists() {
        Employee employee = createEmployee(9L, "omar@example.com", EmployeeStatus.ACTIVE);

        when(employeeValidator.validateEmployeeExists(9L)).thenReturn(employee);

        employeeService.deleteEmployee(9L);

        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.TERMINATED);
        verify(employeeRepository).save(employee);
    }

    @Test
    void bulkCreateEmployeesShouldReturnPartialSuccessWhenSomeRequestsFail() {
        EmployeeServiceImpl employeeServiceSpy = spy(employeeService);
        EmployeeCreateRequest validRequest = new EmployeeCreateRequest(
                "Omar",
                "Hany",
                "omar@example.com",
                null,
                LocalDate.of(2024, 1, 1),
                BigDecimal.valueOf(15000),
                EmployeeStatus.ACTIVE,
                "Backend Developer",
                1L
        );
        EmployeeCreateRequest duplicateRequest = new EmployeeCreateRequest(
                "Sara",
                "Nabil",
                "sara@example.com",
                null,
                LocalDate.of(2024, 1, 2),
                BigDecimal.valueOf(12000),
                EmployeeStatus.ACTIVE,
                "QA Engineer",
                1L
        );
        TransactionStatus transactionStatus = mock(TransactionStatus.class);

        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(employeeValidator.validateBulkCreateRequest(validRequest)).thenReturn(List.of());
        when(employeeValidator.validateBulkCreateRequest(duplicateRequest)).thenReturn(List.of());
        doReturn(createEmployeeResponse(1L, "omar@example.com", EmployeeStatus.ACTIVE))
                .when(employeeServiceSpy).createEmployee(validRequest);
        doThrow(new DuplicateResourceException("Employee email already exists"))
                .when(employeeServiceSpy).createEmployee(duplicateRequest);

        BulkOperationResponse response = employeeServiceSpy.bulkCreateEmployees(List.of(validRequest, duplicateRequest));

        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getFailedCount()).isEqualTo(1);
        assertThat(response.getErrors())
                .singleElement()
                .satisfies(error -> {
                    assertThat(error.getIdentifier()).isEqualTo("sara@example.com");
                    assertThat(error.getReason()).isEqualTo("Employee email already exists");
                });
    }

    @Test
    void bulkUpdateEmployeeStatusShouldReturnPartialSuccessWhenSomeIdsFail() {
        EmployeeServiceImpl employeeServiceSpy = spy(employeeService);
        BulkEmployeeStatusUpdateRequest request =
                new BulkEmployeeStatusUpdateRequest(List.of(1L, 2L, 3L), EmployeeStatus.INACTIVE);
        TransactionStatus transactionStatus = mock(TransactionStatus.class);

        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        doReturn(createEmployeeResponse(1L, "omar@example.com", EmployeeStatus.INACTIVE))
                .when(employeeServiceSpy).changeEmployeeStatus(eq(1L), any(EmployeeStatusUpdateRequest.class));
        doThrow(new ResourceNotFoundException("Employee not found with id: 2"))
                .when(employeeServiceSpy).changeEmployeeStatus(eq(2L), any(EmployeeStatusUpdateRequest.class));
        doReturn(createEmployeeResponse(3L, "sara@example.com", EmployeeStatus.INACTIVE))
                .when(employeeServiceSpy).changeEmployeeStatus(eq(3L), any(EmployeeStatusUpdateRequest.class));

        BulkOperationResponse response = employeeServiceSpy.bulkUpdateEmployeeStatus(request);

        assertThat(response.getSuccessCount()).isEqualTo(2);
        assertThat(response.getFailedCount()).isEqualTo(1);
        assertThat(response.getErrors())
                .singleElement()
                .satisfies(error -> {
                    assertThat(error.getIdentifier()).isEqualTo("2");
                    assertThat(error.getReason()).isEqualTo("Employee not found with id: 2");
                });
    }

    private Department createDepartment(Long id, String name, String code) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setCode(code);
        return department;
    }

    private Employee createEmployee(Long id, String email, EmployeeStatus status) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setEmail(email);
        employee.setStatus(status);
        employee.setDepartment(createDepartment(1L, "Engineering", "ENG"));
        return employee;
    }

    private EmployeeResponse createEmployeeResponse(Long id, String email, EmployeeStatus status) {
        return new EmployeeResponse(id, "Omar", "Hany", email, null, null, null, status, null, null, null, null);
    }
}
