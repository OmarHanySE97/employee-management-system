package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.filter.EmployeeFilterRequest;
import com.example.employeemanagement.dto.request.BulkEmployeeStatusUpdateRequest;
import com.example.employeemanagement.dto.request.EmployeeCreateRequest;
import com.example.employeemanagement.dto.request.EmployeeStatusUpdateRequest;
import com.example.employeemanagement.dto.request.EmployeeUpdateRequest;
import com.example.employeemanagement.dto.response.BulkOperationResponse;
import com.example.employeemanagement.dto.response.EmployeeResponse;
import com.example.employeemanagement.service.EmployeeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for employee management, filtering, and bulk operations.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * Creates a single employee record.
     *
     * @param request the employee creation payload
     * @return the created employee response
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody EmployeeCreateRequest request
    ) {
        EmployeeResponse response = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Processes a batch of employee creation requests with partial success support.
     *
     * @param requests the employee creation payloads
     * @return the bulk operation summary
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<BulkOperationResponse> bulkCreateEmployees(
            @RequestBody List<EmployeeCreateRequest> requests
    ) {
        return ResponseEntity.ok(employeeService.bulkCreateEmployees(requests));
    }

    /**
     * Retrieves employees using pagination, sorting, and optional filters.
     *
     * @param filterRequest filter criteria bound from query parameters
     * @param pageable paging and sorting configuration
     * @return a page of employee responses
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'VIEWER')")
    public ResponseEntity<Page<EmployeeResponse>> getEmployees(
            @Valid @ModelAttribute EmployeeFilterRequest filterRequest,
            @PageableDefault(sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(employeeService.getEmployees(filterRequest, pageable));
    }

    /**
     * Retrieves a single employee by identifier.
     *
     * @param id the employee identifier
     * @return the employee response
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'VIEWER')")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    /**
     * Updates an existing employee record.
     *
     * @param id the employee identifier
     * @param request the employee update payload
     * @return the updated employee response
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequest request
    ) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    /**
     * Changes the status of a single employee.
     *
     * @param id the employee identifier
     * @param request the requested status change
     * @return the updated employee response
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<EmployeeResponse> changeEmployeeStatus(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(employeeService.changeEmployeeStatus(id, request));
    }

    /**
     * Processes a batch employee status update with partial success support.
     *
     * @param request the bulk status update payload
     * @return the bulk operation summary
     */
    @PatchMapping("/bulk/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<BulkOperationResponse> bulkUpdateEmployeeStatus(
            @Valid @RequestBody BulkEmployeeStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(employeeService.bulkUpdateEmployeeStatus(request));
    }

    /**
     * Soft-deletes an employee by moving the status to terminated.
     *
     * @param id the employee identifier
     * @return an empty response with no-content status
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
}
