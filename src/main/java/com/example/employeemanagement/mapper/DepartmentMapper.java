package com.example.employeemanagement.mapper;

import com.example.employeemanagement.dto.response.DepartmentResponse;
import com.example.employeemanagement.dto.response.DepartmentSummaryResponse;
import com.example.employeemanagement.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DepartmentMapper {

    DepartmentResponse toResponse(Department department);

    DepartmentSummaryResponse toSummaryResponse(Department department);
}
