package com.example.employeemanagement.validation;

import com.example.employeemanagement.entity.Department;
import com.example.employeemanagement.exception.BusinessException;
import com.example.employeemanagement.exception.DuplicateResourceException;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.repository.DepartmentRepository;
import com.example.employeemanagement.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Encapsulates department-related business validations and guard checks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DepartmentValidator {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Validates that no department already exists with the supplied name.
     *
     * @param name the department name to validate
     */
    public void validateNameUniqueness(String name) {
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            log.warn("Duplicate department name creation attempt: name={}", name);
            throw new DuplicateResourceException("Department name already exists");
        }
    }

    /**
     * Validates that no department already exists with the supplied code.
     *
     * @param code the department code to validate
     */
    public void validateCodeUniqueness(String code) {
        if (departmentRepository.existsByCodeIgnoreCase(code)) {
            log.warn("Duplicate department code creation attempt: code={}", code);
            throw new DuplicateResourceException("Department code already exists");
        }
    }

    /**
     * Validates that no other department already exists with the supplied name.
     *
     * @param name the department name to validate
     * @param id the department identifier to exclude
     */
    public void validateNameUniqueness(String name, Long id) {
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            log.warn("Duplicate department name update attempt: id={}, name={}", id, name);
            throw new DuplicateResourceException("Department name already exists");
        }
    }

    /**
     * Validates that no other department already exists with the supplied code.
     *
     * @param code the department code to validate
     * @param id the department identifier to exclude
     */
    public void validateCodeUniqueness(String code, Long id) {
        if (departmentRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            log.warn("Duplicate department code update attempt: id={}, code={}", id, code);
            throw new DuplicateResourceException("Department code already exists");
        }
    }

    /**
     * Retrieves a department by identifier or raises a not-found exception.
     *
     * @param id the department identifier
     * @return the existing department entity
     */
    public Department validateDepartmentExists(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }

    /**
     * Validates that the department can be deleted because no employees are assigned to it.
     *
     * @param department the department targeted for deletion
     */
    public void validateDepartmentCanBeDeleted(Department department) {
        if (employeeRepository.existsByDepartmentId(department.getId())) {
            log.warn("Department delete blocked because employees are assigned: id={}, name={}, code={}",
                    department.getId(),
                    department.getName(),
                    department.getCode());
            throw new BusinessException("Department cannot be deleted because employees are assigned to it");
        }
    }
}
