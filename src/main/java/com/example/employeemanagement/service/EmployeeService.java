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

public interface EmployeeService {

    EmployeeResponse createEmployee(EmployeeCreateRequest request);

    BulkOperationResponse bulkCreateEmployees(List<EmployeeCreateRequest> requests);

    Page<EmployeeResponse> getEmployees(EmployeeFilterRequest filterRequest, Pageable pageable);

    EmployeeResponse getEmployeeById(Long id);

    EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request);

    EmployeeResponse changeEmployeeStatus(Long id, EmployeeStatusUpdateRequest request);

    BulkOperationResponse bulkUpdateEmployeeStatus(BulkEmployeeStatusUpdateRequest request);

    void deleteEmployee(Long id);
}
