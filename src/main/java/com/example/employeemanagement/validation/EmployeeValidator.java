package com.example.employeemanagement.validation;

import com.example.employeemanagement.dto.request.BulkEmployeeStatusUpdateRequest;
import com.example.employeemanagement.dto.request.EmployeeCreateRequest;
import com.example.employeemanagement.entity.Department;
import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.enums.EmployeeStatus;
import com.example.employeemanagement.exception.BusinessException;
import com.example.employeemanagement.exception.DuplicateResourceException;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.List;
import com.example.employeemanagement.repository.DepartmentRepository;
import com.example.employeemanagement.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Encapsulates employee-related business validations and guard checks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeValidator {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final Validator beanValidator;

    /**
     * Validates a bulk employee creation payload using bean validation rules.
     *
     * @param request the employee creation payload
     * @return the validation messages collected for the payload
     */
    public List<String> validateBulkCreateRequest(EmployeeCreateRequest request) {
        if (request == null) {
            return List.of("Employee request must not be null");
        }

        return beanValidator.validate(request)
                .stream()
                .sorted(Comparator.comparing((ConstraintViolation<EmployeeCreateRequest> violation) ->
                                violation.getPropertyPath().toString())
                        .thenComparing(ConstraintViolation::getMessage))
                .map(ConstraintViolation::getMessage)
                .toList();
    }

    /**
     * Validates the top-level bulk employee status update payload.
     *
     * @param request the bulk status update payload
     */
    public void validateBulkStatusUpdateRequest(BulkEmployeeStatusUpdateRequest request) {
        if (request == null) {
            throw new BusinessException("Bulk employee status update request is required");
        }

        if (request.getEmployeeIds() == null || request.getEmployeeIds().isEmpty()) {
            throw new BusinessException("Employee ids are required");
        }

        if (request.getStatus() == null) {
            throw new BusinessException("Status is required");
        }
    }

    /**
     * Validates that no employee already exists with the supplied email.
     *
     * @param email the employee email to validate
     */
    public void validateEmailUniqueness(String email) {
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Duplicate employee email creation attempt: email={}", email);
            throw new DuplicateResourceException("Employee email already exists");
        }
    }

    /**
     * Validates that no other employee already exists with the supplied email.
     *
     * @param email the employee email to validate
     * @param id the employee identifier to exclude
     */
    public void validateEmailUniqueness(String email, Long id) {
        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            log.warn("Duplicate employee email update attempt: id={}, email={}", id, email);
            throw new DuplicateResourceException("Employee email already exists");
        }
    }

    /**
     * Retrieves an employee by identifier or raises a not-found exception.
     *
     * @param id the employee identifier
     * @return the existing employee entity
     */
    public Employee validateEmployeeExists(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    /**
     * Retrieves a department by identifier or raises a not-found exception.
     *
     * @param departmentId the department identifier
     * @return the existing department entity
     */
    public Department validateDepartmentExists(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> {
                    log.warn("Employee operation failed because department was not found: departmentId={}", departmentId);
                    return new ResourceNotFoundException("Department not found with id: " + departmentId);
                });
    }

    /**
     * Validates whether the requested employee status transition is allowed.
     *
     * @param employee the employee being updated
     * @param newStatus the requested new status
     */
    public void validateStatusTransition(Employee employee, EmployeeStatus newStatus) {
        if (employee.getStatus() == EmployeeStatus.TERMINATED && newStatus == EmployeeStatus.ACTIVE) {
            log.warn("Employee status change blocked by business rule: id={}, currentStatus={}, requestedStatus={}",
                    employee.getId(),
                    employee.getStatus(),
                    newStatus);
            throw new BusinessException("Terminated employee cannot be reactivated");
        }
    }
}
