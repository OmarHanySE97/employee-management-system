package com.example.employeemanagement.mapper;

import com.example.employeemanagement.dto.response.EmployeeResponse;
import com.example.employeemanagement.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = DepartmentMapper.class
)
public interface EmployeeMapper {

    EmployeeResponse toResponse(Employee employee);
}
