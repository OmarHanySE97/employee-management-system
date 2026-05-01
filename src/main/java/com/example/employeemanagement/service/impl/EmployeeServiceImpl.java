package com.example.employeemanagement.service.impl;

import com.example.employeemanagement.dto.filter.EmployeeFilterRequest;
import com.example.employeemanagement.dto.request.BulkEmployeeStatusUpdateRequest;
import com.example.employeemanagement.dto.request.EmployeeCreateRequest;
import com.example.employeemanagement.dto.request.EmployeeStatusUpdateRequest;
import com.example.employeemanagement.dto.request.EmployeeUpdateRequest;
import com.example.employeemanagement.dto.response.BulkOperationErrorResponse;
import com.example.employeemanagement.dto.response.BulkOperationResponse;
import com.example.employeemanagement.dto.response.EmployeeResponse;
import com.example.employeemanagement.entity.Department;
import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.enums.EmployeeStatus;
import com.example.employeemanagement.exception.BusinessException;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.mapper.EmployeeMapper;
import com.example.employeemanagement.repository.EmployeeRepository;
import com.example.employeemanagement.service.EmployeeService;
import com.example.employeemanagement.validation.EmployeeValidator;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final EmployeeValidator employeeValidator;
    private final PlatformTransactionManager transactionManager;

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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BulkOperationResponse bulkCreateEmployees(List<EmployeeCreateRequest> requests) {
        if (requests == null) {
            throw new BusinessException("Employee requests are required");
        }

        List<BulkOperationErrorResponse> errors = new ArrayList<>();
        int successCount = 0;
        TransactionTemplate transactionTemplate = createRequiresNewTransactionTemplate();

        for (int index = 0; index < requests.size(); index++) {
            EmployeeCreateRequest request = requests.get(index);
            String identifier = resolveBulkCreateIdentifier(request, index);
            List<String> validationErrors = employeeValidator.validateBulkCreateRequest(request);

            if (!validationErrors.isEmpty()) {
                errors.add(new BulkOperationErrorResponse(identifier, String.join(", ", validationErrors)));
                continue;
            }

            try {
                transactionTemplate.executeWithoutResult(status -> createEmployee(request));
                successCount++;
            } catch (ResourceNotFoundException
                     | BusinessException
                     | DataIntegrityViolationException exception) {
                errors.add(new BulkOperationErrorResponse(identifier, resolveBulkOperationReason(exception)));
            } catch (Exception exception) {
                log.error("Unexpected error during bulk employee creation: identifier={}", identifier, exception);
                errors.add(new BulkOperationErrorResponse(identifier, "Unexpected error occurred"));
            }
        }

        BulkOperationResponse response = new BulkOperationResponse(successCount, errors.size(), errors);
        log.info("Bulk employee creation completed: requested={}, successCount={}, failedCount={}",
                requests.size(),
                response.getSuccessCount(),
                response.getFailedCount());

        return response;
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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BulkOperationResponse bulkUpdateEmployeeStatus(BulkEmployeeStatusUpdateRequest request) {
        employeeValidator.validateBulkStatusUpdateRequest(request);

        List<BulkOperationErrorResponse> errors = new ArrayList<>();
        int successCount = 0;
        TransactionTemplate transactionTemplate = createRequiresNewTransactionTemplate();
        EmployeeStatusUpdateRequest statusUpdateRequest = new EmployeeStatusUpdateRequest(request.getStatus());

        for (Long employeeId : request.getEmployeeIds()) {
            if (employeeId == null) {
                errors.add(new BulkOperationErrorResponse("null", "Employee id must not be null"));
                continue;
            }

            String identifier = String.valueOf(employeeId);

            try {
                transactionTemplate.executeWithoutResult(status ->
                        changeEmployeeStatus(employeeId, statusUpdateRequest)
                );
                successCount++;
            } catch (ResourceNotFoundException
                     | BusinessException
                     | DataIntegrityViolationException exception) {
                errors.add(new BulkOperationErrorResponse(identifier, resolveBulkOperationReason(exception)));
            } catch (Exception exception) {
                log.error("Unexpected error during bulk employee status update: employeeId={}", employeeId, exception);
                errors.add(new BulkOperationErrorResponse(identifier, "Unexpected error occurred"));
            }
        }

        BulkOperationResponse response = new BulkOperationResponse(successCount, errors.size(), errors);
        log.info("Bulk employee status update completed: requested={}, successCount={}, failedCount={}, targetStatus={}",
                request.getEmployeeIds().size(),
                response.getSuccessCount(),
                response.getFailedCount(),
                request.getStatus());

        return response;
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

    private TransactionTemplate createRequiresNewTransactionTemplate() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate;
    }

    private String resolveBulkCreateIdentifier(EmployeeCreateRequest request, int index) {
        if (request != null && StringUtils.hasText(request.getEmail())) {
            return request.getEmail().trim();
        }
        return "row-" + (index + 1);
    }

    private String resolveBulkOperationReason(Exception exception) {
        if (exception instanceof DataIntegrityViolationException) {
            return "The request could not be completed because it conflicts with existing data";
        }

        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }

        return "Unexpected error occurred";
    }
}
