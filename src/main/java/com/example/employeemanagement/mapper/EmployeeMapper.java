package com.example.employeemanagement.mapper;

import com.example.employeemanagement.dto.response.EmployeeResponse;
import com.example.employeemanagement.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Maps employee entities to API response DTOs.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = DepartmentMapper.class
)
public interface EmployeeMapper {

    /**
     * Maps an employee entity to the detailed response representation.
     *
     * @param employee the source employee entity
     * @return the mapped employee response
     */
    EmployeeResponse toResponse(Employee employee);
}
