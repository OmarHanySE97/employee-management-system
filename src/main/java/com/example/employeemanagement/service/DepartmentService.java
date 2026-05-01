package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.request.DepartmentCreateRequest;
import com.example.employeemanagement.dto.request.DepartmentUpdateRequest;
import com.example.employeemanagement.dto.response.DepartmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Defines department management use cases.
 */
public interface DepartmentService {

    /**
     * Creates a new department.
     *
     * @param request the department creation payload
     * @return the created department response
     */
    DepartmentResponse createDepartment(DepartmentCreateRequest request);

    /**
     * Retrieves departments using pagination and sorting options.
     *
     * @param pageable paging and sorting configuration
     * @return a page of departments
     */
    Page<DepartmentResponse> getDepartments(Pageable pageable);

    /**
     * Retrieves a department by identifier.
     *
     * @param id the department identifier
     * @return the matching department response
     */
    DepartmentResponse getDepartmentById(Long id);

    /**
     * Updates an existing department.
     *
     * @param id the department identifier
     * @param request the update payload
     * @return the updated department response
     */
    DepartmentResponse updateDepartment(Long id, DepartmentUpdateRequest request);

    /**
     * Deletes a department when business rules allow it.
     *
     * @param id the department identifier
     */
    void deleteDepartment(Long id);
}
