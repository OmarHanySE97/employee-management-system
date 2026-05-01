package com.example.employeemanagement.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.employeemanagement.dto.request.DepartmentCreateRequest;
import com.example.employeemanagement.dto.request.DepartmentUpdateRequest;
import com.example.employeemanagement.dto.response.DepartmentResponse;
import com.example.employeemanagement.entity.Department;
import com.example.employeemanagement.exception.BusinessException;
import com.example.employeemanagement.exception.DuplicateResourceException;
import com.example.employeemanagement.mapper.DepartmentMapper;
import com.example.employeemanagement.repository.DepartmentRepository;
import com.example.employeemanagement.validation.DepartmentValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private DepartmentValidator departmentValidator;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    @Test
    void createDepartmentShouldReturnResponseWhenRequestIsValid() {
        DepartmentCreateRequest request = new DepartmentCreateRequest(" Engineering ", " ENG ", " Core team ");
        DepartmentResponse expectedResponse = new DepartmentResponse(1L, "Engineering", "ENG", "Core team", null, null);

        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department department = invocation.getArgument(0);
            department.setId(1L);
            return department;
        });
        when(departmentMapper.toResponse(any(Department.class))).thenReturn(expectedResponse);

        DepartmentResponse actualResponse = departmentService.createDepartment(request);

        assertThat(actualResponse).isSameAs(expectedResponse);
        verify(departmentValidator).validateNameUniqueness("Engineering");
        verify(departmentValidator).validateCodeUniqueness("ENG");

        ArgumentCaptor<Department> departmentCaptor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(departmentCaptor.capture());
        Department savedDepartment = departmentCaptor.getValue();
        assertThat(savedDepartment.getName()).isEqualTo("Engineering");
        assertThat(savedDepartment.getCode()).isEqualTo("ENG");
        assertThat(savedDepartment.getDescription()).isEqualTo("Core team");
    }

    @Test
    void createDepartmentShouldThrowWhenDepartmentCodeAlreadyExists() {
        DepartmentCreateRequest request = new DepartmentCreateRequest("Product", "ENG", null);
        DuplicateResourceException exception = new DuplicateResourceException("Department code already exists");

        doThrow(exception).when(departmentValidator).validateCodeUniqueness("ENG");

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isSameAs(exception);

        verify(departmentValidator).validateNameUniqueness("Product");
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    void createDepartmentShouldThrowWhenDepartmentNameAlreadyExists() {
        DepartmentCreateRequest request = new DepartmentCreateRequest("Engineering", "PRD", null);
        DuplicateResourceException exception = new DuplicateResourceException("Department name already exists");

        doThrow(exception).when(departmentValidator).validateNameUniqueness("Engineering");

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isSameAs(exception);

        verify(departmentValidator, never()).validateCodeUniqueness("PRD");
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    void updateDepartmentShouldReturnResponseWhenRequestIsValid() {
        Department department = createDepartment(3L, "Engineering", "ENG");
        DepartmentUpdateRequest request = new DepartmentUpdateRequest(" Operations ", " OPS ", " Updated ");
        DepartmentResponse expectedResponse = new DepartmentResponse(3L, "Operations", "OPS", "Updated", null, null);

        when(departmentValidator.validateDepartmentExists(3L)).thenReturn(department);
        when(departmentRepository.save(department)).thenReturn(department);
        when(departmentMapper.toResponse(department)).thenReturn(expectedResponse);

        DepartmentResponse actualResponse = departmentService.updateDepartment(3L, request);

        assertThat(actualResponse).isSameAs(expectedResponse);
        verify(departmentValidator).validateNameUniqueness("Operations", 3L);
        verify(departmentValidator).validateCodeUniqueness("OPS", 3L);
        assertThat(department.getName()).isEqualTo("Operations");
        assertThat(department.getCode()).isEqualTo("OPS");
        assertThat(department.getDescription()).isEqualTo("Updated");
    }

    @Test
    void deleteDepartmentShouldDeleteDepartmentWhenNoEmployeesAreAssigned() {
        Department department = createDepartment(5L, "Finance", "FIN");

        when(departmentValidator.validateDepartmentExists(5L)).thenReturn(department);

        departmentService.deleteDepartment(5L);

        verify(departmentValidator).validateDepartmentCanBeDeleted(department);
        verify(departmentRepository).delete(department);
    }

    @Test
    void deleteDepartmentShouldThrowWhenEmployeesAreAssigned() {
        Department department = createDepartment(5L, "Finance", "FIN");
        BusinessException exception =
                new BusinessException("Department cannot be deleted because employees are assigned to it");

        when(departmentValidator.validateDepartmentExists(5L)).thenReturn(department);
        doThrow(exception).when(departmentValidator).validateDepartmentCanBeDeleted(department);

        assertThatThrownBy(() -> departmentService.deleteDepartment(5L))
                .isSameAs(exception);

        verify(departmentRepository, never()).delete(any(Department.class));
    }

    private Department createDepartment(Long id, String name, String code) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setCode(code);
        return department;
    }
}
