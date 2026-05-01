package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.filter.EmployeeFilterRequest;
import com.example.employeemanagement.dto.request.BulkEmployeeStatusUpdateRequest;
import com.example.employeemanagement.dto.request.EmployeeCreateRequest;
import com.example.employeemanagement.dto.request.EmployeeStatusUpdateRequest;
import com.example.employeemanagement.dto.request.EmployeeUpdateRequest;
import com.example.employeemanagement.dto.response.BulkOperationResponse;
import com.example.employeemanagement.dto.response.EmployeeResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Defines employee management, filtering, and bulk operation use cases.
 */
public interface EmployeeService {

    /**
     * Creates a single employee record.
     *
     * @param request the employee creation payload
     * @return the created employee response
     */
    EmployeeResponse createEmployee(EmployeeCreateRequest request);

    /**
     * Processes a batch of employee creation requests with partial success handling.
     *
     * @param requests the employee creation payloads
     * @return the bulk operation result summary
     */
    BulkOperationResponse bulkCreateEmployees(List<EmployeeCreateRequest> requests);

    /**
     * Retrieves employees using filters, pagination, and sorting.
     *
     * @param filterRequest the filter criteria
     * @param pageable paging and sorting configuration
     * @return a page of employee responses
     */
    Page<EmployeeResponse> getEmployees(EmployeeFilterRequest filterRequest, Pageable pageable);

    /**
     * Retrieves a single employee by identifier.
     *
     * @param id the employee identifier
     * @return the matching employee response
     */
    EmployeeResponse getEmployeeById(Long id);

    /**
     * Updates an existing employee record.
     *
     * @param id the employee identifier
     * @param request the update payload
     * @return the updated employee response
     */
    EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request);

    /**
     * Changes the status of a single employee.
     *
     * @param id the employee identifier
     * @param request the requested status update
     * @return the updated employee response
     */
    EmployeeResponse changeEmployeeStatus(Long id, EmployeeStatusUpdateRequest request);

    /**
     * Processes a batch employee status update with partial success handling.
     *
     * @param request the bulk status update payload
     * @return the bulk operation result summary
     */
    BulkOperationResponse bulkUpdateEmployeeStatus(BulkEmployeeStatusUpdateRequest request);

    /**
     * Soft-deletes an employee record.
     *
     * @param id the employee identifier
     */
    void deleteEmployee(Long id);
}
