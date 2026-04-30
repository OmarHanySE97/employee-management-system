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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    private final DepartmentValidator departmentValidator;

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

    @Override
    public Page<DepartmentResponse> getDepartments(Pageable pageable) {
        return departmentRepository.findAll(pageable)
                .map(departmentMapper::toResponse);
    }

    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        return departmentMapper.toResponse(departmentValidator.validateDepartmentExists(id));
    }

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

    private String normalizeRequiredValue(String value) {
        return value.trim();
    }

    private String normalizeOptionalValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
