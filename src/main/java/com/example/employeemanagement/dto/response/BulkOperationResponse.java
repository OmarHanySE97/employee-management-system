package com.example.employeemanagement.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkOperationResponse {

    private int successCount;

    private int failedCount;

    private List<BulkOperationErrorResponse> errors;
}
