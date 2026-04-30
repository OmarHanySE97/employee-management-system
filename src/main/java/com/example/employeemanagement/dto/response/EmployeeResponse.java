package com.example.employeemanagement.dto.response;

import com.example.employeemanagement.enums.EmployeeStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String phoneNumber;

    private LocalDate hireDate;

    private BigDecimal salary;

    private EmployeeStatus status;

    private String jobTitle;

    private DepartmentSummaryResponse department;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
