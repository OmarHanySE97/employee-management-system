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

@Slf4j
@Component
@RequiredArgsConstructor
public class DepartmentValidator {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public void validateNameUniqueness(String name) {
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            log.warn("Duplicate department name creation attempt: name={}", name);
            throw new DuplicateResourceException("Department name already exists");
        }
    }

    public void validateCodeUniqueness(String code) {
        if (departmentRepository.existsByCodeIgnoreCase(code)) {
            log.warn("Duplicate department code creation attempt: code={}", code);
            throw new DuplicateResourceException("Department code already exists");
        }
    }

    public void validateNameUniqueness(String name, Long id) {
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            log.warn("Duplicate department name update attempt: id={}, name={}", id, name);
            throw new DuplicateResourceException("Department name already exists");
        }
    }

    public void validateCodeUniqueness(String code, Long id) {
        if (departmentRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            log.warn("Duplicate department code update attempt: id={}, code={}", id, code);
            throw new DuplicateResourceException("Department code already exists");
        }
    }

    public Department validateDepartmentExists(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }

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
