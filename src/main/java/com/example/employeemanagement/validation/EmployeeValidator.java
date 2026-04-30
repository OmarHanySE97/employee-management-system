package com.example.employeemanagement.validation;

import com.example.employeemanagement.entity.Department;
import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.enums.EmployeeStatus;
import com.example.employeemanagement.exception.BusinessException;
import com.example.employeemanagement.exception.DuplicateResourceException;
import com.example.employeemanagement.exception.ResourceNotFoundException;
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
