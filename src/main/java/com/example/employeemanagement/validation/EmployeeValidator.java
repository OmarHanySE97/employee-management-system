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

@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeValidator {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final Validator beanValidator;

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

    public void validateEmailUniqueness(String email) {
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Duplicate employee email creation attempt: email={}", email);
            throw new DuplicateResourceException("Employee email already exists");
        }
    }

    public void validateEmailUniqueness(String email, Long id) {
        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            log.warn("Duplicate employee email update attempt: id={}, email={}", id, email);
            throw new DuplicateResourceException("Employee email already exists");
        }
    }

    public Employee validateEmployeeExists(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    public Department validateDepartmentExists(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> {
                    log.warn("Employee operation failed because department was not found: departmentId={}", departmentId);
                    return new ResourceNotFoundException("Department not found with id: " + departmentId);
                });
    }

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
