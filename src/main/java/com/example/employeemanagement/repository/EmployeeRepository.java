package com.example.employeemanagement.repository;

import com.example.employeemanagement.entity.Employee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repository for employee persistence, existence checks, and filtered reads.
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    /**
     * Checks whether an employee already exists with the supplied email ignoring case.
     *
     * @param email the email address to check
     * @return {@code true} when a duplicate exists
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Checks whether another employee already uses the supplied email ignoring case.
     *
     * @param email the email address to check
     * @param id the identifier to exclude from the duplicate search
     * @return {@code true} when a duplicate exists outside the excluded record
     */
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /**
     * Checks whether a department currently has at least one employee assigned to it.
     *
     * @param departmentId the department identifier
     * @return {@code true} when the department has employees
     */
    boolean existsByDepartmentId(Long departmentId);

    /**
     * Retrieves all employees with their departments eagerly loaded.
     *
     * @return all employees
     */
    @Override
    @EntityGraph(attributePaths = "department")
    List<Employee> findAll();

    /**
     * Retrieves a single employee with its department eagerly loaded.
     *
     * @param id the employee identifier
     * @return the matching employee if it exists
     */
    @Override
    @EntityGraph(attributePaths = "department")
    Optional<Employee> findById(Long id);

    /**
     * Retrieves employees matching the supplied specification with departments eagerly loaded.
     *
     * @param specification the filter specification to apply
     * @return the matching employees
     */
    @EntityGraph(attributePaths = "department")
    List<Employee> findAll(Specification<Employee> specification);

    /**
     * Retrieves a paged employee result matching the supplied specification with departments eagerly loaded.
     *
     * @param specification the filter specification to apply
     * @param pageable paging and sorting configuration
     * @return the matching page of employees
     */
    @Override
    @EntityGraph(attributePaths = "department")
    Page<Employee> findAll(Specification<Employee> specification, Pageable pageable);
}
