package com.example.employeemanagement.service.impl;

import com.example.employeemanagement.dto.filter.EmployeeFilterRequest;
import com.example.employeemanagement.dto.request.EmployeeCreateRequest;
import com.example.employeemanagement.dto.request.EmployeeStatusUpdateRequest;
import com.example.employeemanagement.dto.request.EmployeeUpdateRequest;
import com.example.employeemanagement.dto.response.EmployeeResponse;
import com.example.employeemanagement.entity.Department;
import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.enums.EmployeeStatus;
import com.example.employeemanagement.mapper.EmployeeMapper;
import com.example.employeemanagement.repository.EmployeeRepository;
import com.example.employeemanagement.service.EmployeeService;
import com.example.employeemanagement.validation.EmployeeValidator;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final EmployeeValidator employeeValidator;

    @Override
    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        String email = normalizeRequiredValue(request.getEmail());
        employeeValidator.validateEmailUniqueness(email);

        Department department = employeeValidator.validateDepartmentExists(request.getDepartmentId());

        Employee employee = new Employee();
        employee.setFirstName(normalizeRequiredValue(request.getFirstName()));
        employee.setLastName(normalizeRequiredValue(request.getLastName()));
        employee.setEmail(email);
        employee.setPhoneNumber(normalizeOptionalValue(request.getPhoneNumber()));
        employee.setHireDate(request.getHireDate());
        employee.setSalary(request.getSalary());
        employee.setStatus(request.getStatus() != null ? request.getStatus() : EmployeeStatus.ACTIVE);
        employee.setJobTitle(normalizeOptionalValue(request.getJobTitle()));
        employee.setDepartment(department);

        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employee created successfully: id={}, email={}, departmentId={}, status={}",
                savedEmployee.getId(),
                savedEmployee.getEmail(),
                savedEmployee.getDepartment().getId(),
                savedEmployee.getStatus());

        return employeeMapper.toResponse(savedEmployee);
    }

    @Override
    public Page<EmployeeResponse> getEmployees(EmployeeFilterRequest filterRequest, Pageable pageable) {
        Specification<Employee> specification = buildSpecification(filterRequest);
        return employeeRepository.findAll(specification, pageable)
                .map(employeeMapper::toResponse);
    }

    @Override
    public EmployeeResponse getEmployeeById(Long id) {
        return employeeMapper.toResponse(employeeValidator.validateEmployeeExists(id));
    }

    @Override
    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        Employee employee = employeeValidator.validateEmployeeExists(id);
        String email = normalizeRequiredValue(request.getEmail());
        employeeValidator.validateEmailUniqueness(email, id);

        Department department = employeeValidator.validateDepartmentExists(request.getDepartmentId());

        employee.setFirstName(normalizeRequiredValue(request.getFirstName()));
        employee.setLastName(normalizeRequiredValue(request.getLastName()));
        employee.setEmail(email);
        employee.setPhoneNumber(normalizeOptionalValue(request.getPhoneNumber()));
        employee.setHireDate(request.getHireDate());
        employee.setSalary(request.getSalary());
        employee.setStatus(request.getStatus());
        employee.setJobTitle(normalizeOptionalValue(request.getJobTitle()));
        employee.setDepartment(department);

        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employee updated successfully: id={}, email={}, departmentId={}, status={}",
                savedEmployee.getId(),
                savedEmployee.getEmail(),
                savedEmployee.getDepartment().getId(),
                savedEmployee.getStatus());

        return employeeMapper.toResponse(savedEmployee);
    }

    @Override
    @Transactional
    public EmployeeResponse changeEmployeeStatus(Long id, EmployeeStatusUpdateRequest request) {
        Employee employee = employeeValidator.validateEmployeeExists(id);
        EmployeeStatus currentStatus = employee.getStatus();
        EmployeeStatus newStatus = request.getStatus();

        employeeValidator.validateStatusTransition(employee, newStatus);

        employee.setStatus(newStatus);
        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employee status changed successfully: id={}, fromStatus={}, toStatus={}",
                savedEmployee.getId(),
                currentStatus,
                savedEmployee.getStatus());

        return employeeMapper.toResponse(savedEmployee);
    }

    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeValidator.validateEmployeeExists(id);
        EmployeeStatus previousStatus = employee.getStatus();

        employee.setStatus(EmployeeStatus.TERMINATED);
        employeeRepository.save(employee);
        log.info("Employee soft deleted successfully: id={}, email={}, previousStatus={}, currentStatus={}",
                employee.getId(),
                employee.getEmail(),
                previousStatus,
                employee.getStatus());
    }

    private Specification<Employee> buildSpecification(EmployeeFilterRequest filterRequest) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filterRequest != null) {
                if (filterRequest.getStatus() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), filterRequest.getStatus()));
                }

                if (filterRequest.getDepartmentId() != null) {
                    predicates.add(criteriaBuilder.equal(
                            root.get("department").get("id"),
                            filterRequest.getDepartmentId()
                    ));
                }

                if (StringUtils.hasText(filterRequest.getKeyword())) {
                    String keyword = "%" + filterRequest.getKeyword().trim().toLowerCase() + "%";
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), keyword),
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), keyword),
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), keyword),
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("jobTitle")), keyword)
                    ));
                }

                if (filterRequest.getMinSalary() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                            root.get("salary"),
                            filterRequest.getMinSalary()
                    ));
                }

                if (filterRequest.getMaxSalary() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(
                            root.get("salary"),
                            filterRequest.getMaxSalary()
                    ));
                }

                if (filterRequest.getHireDateFrom() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                            root.get("hireDate"),
                            filterRequest.getHireDateFrom()
                    ));
                }

                if (filterRequest.getHireDateTo() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(
                            root.get("hireDate"),
                            filterRequest.getHireDateTo()
                    ));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
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
