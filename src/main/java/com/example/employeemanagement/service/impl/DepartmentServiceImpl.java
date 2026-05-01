package com.example.employeemanagement.service.impl;

import com.example.employeemanagement.dto.request.DepartmentCreateRequest;
import com.example.employeemanagement.dto.request.DepartmentUpdateRequest;
import com.example.employeemanagement.dto.response.DepartmentResponse;
import com.example.employeemanagement.entity.Department;
import com.example.employeemanagement.mapper.DepartmentMapper;
import com.example.employeemanagement.repository.DepartmentRepository;
import com.example.employeemanagement.service.DepartmentService;
import com.example.employeemanagement.validation.DepartmentValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Default implementation of department management use cases.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    private final DepartmentValidator departmentValidator;

    /**
     * Creates a department after validating name and code uniqueness.
     *
     * @param request the department creation payload
     * @return the created department response
     */
    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentCreateRequest request) {
        String name = normalizeRequiredValue(request.getName());
        String code = normalizeRequiredValue(request.getCode());

        departmentValidator.validateNameUniqueness(name);
        departmentValidator.validateCodeUniqueness(code);

        Department department = new Department();
        department.setName(name);
        department.setCode(code);
        department.setDescription(normalizeOptionalValue(request.getDescription()));

        Department savedDepartment = departmentRepository.save(department);
        log.info("Department created successfully: id={}, name={}, code={}",
                savedDepartment.getId(),
                savedDepartment.getName(),
                savedDepartment.getCode());

        return departmentMapper.toResponse(savedDepartment);
    }

    /**
     * Retrieves departments with pagination and sorting applied.
     *
     * @param pageable paging and sorting configuration
     * @return a page of department responses
     */
    @Override
    public Page<DepartmentResponse> getDepartments(Pageable pageable) {
        return departmentRepository.findAll(pageable)
                .map(departmentMapper::toResponse);
    }

    /**
     * Retrieves a single department by identifier.
     *
     * @param id the department identifier
     * @return the matching department response
     */
    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        return departmentMapper.toResponse(departmentValidator.validateDepartmentExists(id));
    }

    /**
     * Updates a department after uniqueness and existence checks pass.
     *
     * @param id the department identifier
     * @param request the update payload
     * @return the updated department response
     */
    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentUpdateRequest request) {
        Department department = departmentValidator.validateDepartmentExists(id);
        String name = normalizeRequiredValue(request.getName());
        String code = normalizeRequiredValue(request.getCode());

        departmentValidator.validateNameUniqueness(name, id);
        departmentValidator.validateCodeUniqueness(code, id);

        department.setName(name);
        department.setCode(code);
        department.setDescription(normalizeOptionalValue(request.getDescription()));

        Department savedDepartment = departmentRepository.save(department);
        log.info("Department updated successfully: id={}, name={}, code={}",
                savedDepartment.getId(),
                savedDepartment.getName(),
                savedDepartment.getCode());

        return departmentMapper.toResponse(savedDepartment);
    }

    /**
     * Deletes a department when no employees are assigned to it.
     *
     * @param id the department identifier
     */
    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        Department department = departmentValidator.validateDepartmentExists(id);
        departmentValidator.validateDepartmentCanBeDeleted(department);

        departmentRepository.delete(department);
        log.info("Department deleted successfully: id={}, name={}, code={}",
                department.getId(),
                department.getName(),
                department.getCode());
    }

    /**
     * Trims a required string value before persistence.
     *
     * @param value the raw input value
     * @return the trimmed value
     */
    private String normalizeRequiredValue(String value) {
        return value.trim();
    }

    /**
     * Trims an optional string value and converts blank input to {@code null}.
     *
     * @param value the raw input value
     * @return the trimmed value or {@code null} when blank
     */
    private String normalizeOptionalValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
