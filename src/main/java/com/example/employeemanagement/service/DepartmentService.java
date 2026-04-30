package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.request.DepartmentCreateRequest;
import com.example.employeemanagement.dto.request.DepartmentUpdateRequest;
import com.example.employeemanagement.dto.response.DepartmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface DepartmentService {

    DepartmentResponse createDepartment(DepartmentCreateRequest request);

    Page<DepartmentResponse> getDepartments(Pageable pageable);

    DepartmentResponse getDepartmentById(Long id);

    DepartmentResponse updateDepartment(Long id, DepartmentUpdateRequest request);

    void deleteDepartment(Long id);
}
