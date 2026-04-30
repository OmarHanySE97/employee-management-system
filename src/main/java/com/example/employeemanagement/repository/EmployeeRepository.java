package com.example.employeemanagement.repository;

import com.example.employeemanagement.entity.Employee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByDepartmentId(Long departmentId);

    @Override
    @EntityGraph(attributePaths = "department")
    List<Employee> findAll();

    @Override
    @EntityGraph(attributePaths = "department")
    Optional<Employee> findById(Long id);

    @EntityGraph(attributePaths = "department")
    List<Employee> findAll(Specification<Employee> specification);
}
