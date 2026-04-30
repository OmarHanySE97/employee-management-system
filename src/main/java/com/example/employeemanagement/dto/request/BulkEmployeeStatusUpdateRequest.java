package com.example.employeemanagement.dto.request;

import com.example.employeemanagement.enums.EmployeeStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkEmployeeStatusUpdateRequest {

    @NotEmpty(message = "Employee ids are required")
    private List<@NotNull(message = "Employee id must not be null") Long> employeeIds;

    @NotNull(message = "Status is required")
    private EmployeeStatus status;
}
