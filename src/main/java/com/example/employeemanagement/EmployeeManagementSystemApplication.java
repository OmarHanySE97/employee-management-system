package com.example.employeemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstraps the Employee Management System Spring Boot application.
 */
@SpringBootApplication
public class EmployeeManagementSystemApplication {

    /**
     * Starts the Spring Boot application context.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(EmployeeManagementSystemApplication.class, args);
    }
}
