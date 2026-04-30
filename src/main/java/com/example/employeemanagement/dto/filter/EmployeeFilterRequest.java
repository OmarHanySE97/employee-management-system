package com.example.employeemanagement.dto.filter;

import com.example.employeemanagement.enums.EmployeeStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeFilterRequest {

    private EmployeeStatus status;

    private Long departmentId;

    private String keyword;

    private BigDecimal minSalary;

    private BigDecimal maxSalary;

    private LocalDate hireDateFrom;

    private LocalDate hireDateTo;
}
