package com.example.employeemanagement.dto.request;

import com.example.employeemanagement.enums.EmployeeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private EmployeeStatus status;
}
