package com.example.employeemanagement.mapper;

import com.example.employeemanagement.dto.response.DepartmentResponse;
import com.example.employeemanagement.dto.response.DepartmentSummaryResponse;
import com.example.employeemanagement.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Maps department entities to API response DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DepartmentMapper {

    /**
     * Maps a department entity to the detailed response representation.
     *
     * @param department the source department entity
     * @return the mapped department response
     */
    DepartmentResponse toResponse(Department department);

    /**
     * Maps a department entity to its compact summary representation.
     *
     * @param department the source department entity
     * @return the mapped department summary response
     */
    DepartmentSummaryResponse toSummaryResponse(Department department);
}
