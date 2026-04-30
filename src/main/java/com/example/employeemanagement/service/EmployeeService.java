package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.filter.EmployeeFilterRequest;
import com.example.employeemanagement.dto.request.EmployeeCreateRequest;
import com.example.employeemanagement.dto.request.EmployeeStatusUpdateRequest;
import com.example.employeemanagement.dto.request.EmployeeUpdateRequest;
import com.example.employeemanagement.dto.response.EmployeeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface EmployeeService {

    EmployeeResponse createEmployee(EmployeeCreateRequest request);

    Page<EmployeeResponse> getEmployees(EmployeeFilterRequest filterRequest, Pageable pageable);

    EmployeeResponse getEmployeeById(Long id);

    EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request);

    EmployeeResponse changeEmployeeStatus(Long id, EmployeeStatusUpdateRequest request);

    void deleteEmployee(Long id);
}
