package com.example.employeemanagement.entity;

import com.example.employeemanagement.enums.EmployeeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Employee entity representing an individual staff member.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "employees",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employees_email", columnNames = "email")
        }
)
public class Employee extends BaseAuditableEntity {

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 30)
    private String phoneNumber;

    @Column(nullable = false)
    private LocalDate hireDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal salary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeStatus status;

    @Column(nullable = false, length = 150)
    private String jobTitle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    /**
     * Returns a safe string representation for debugging and logs without traversing relationships.
     *
     * @return the employee summary string
     */
    @Override
    public String toString() {
        return "Employee{"
                + "id=" + getId()
                + ", firstName='" + firstName + '\''
                + ", lastName='" + lastName + '\''
                + ", email='" + email + '\''
                + ", hireDate=" + hireDate
                + ", salary=" + salary
                + ", status=" + status
                + ", jobTitle='" + jobTitle + '\''
                + '}';
    }
}
