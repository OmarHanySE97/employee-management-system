package com.example.employeemanagement.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponse {

    private Long id;

    private String name;

    private String code;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
