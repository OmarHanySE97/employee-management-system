package com.example.employeemanagement.repository;

import com.example.employeemanagement.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for department persistence and uniqueness checks.
 */
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * Checks whether a department already exists with the supplied name ignoring case.
     *
     * @param name the department name to check
     * @return {@code true} when a duplicate exists
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Checks whether a department already exists with the supplied code ignoring case.
     *
     * @param code the department code to check
     * @return {@code true} when a duplicate exists
     */
    boolean existsByCodeIgnoreCase(String code);

    /**
     * Checks whether another department already uses the supplied name ignoring case.
     *
     * @param name the department name to check
     * @param id the identifier to exclude from the duplicate search
     * @return {@code true} when a duplicate exists outside the excluded record
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Checks whether another department already uses the supplied code ignoring case.
     *
     * @param code the department code to check
     * @param id the identifier to exclude from the duplicate search
     * @return {@code true} when a duplicate exists outside the excluded record
     */
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
