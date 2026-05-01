package com.example.employeemanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Department entity representing an organizational unit.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "departments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_departments_name", columnNames = "name"),
                @UniqueConstraint(name = "uk_departments_code", columnNames = "code")
        }
)
public class Department extends BaseAuditableEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(length = 1000)
    private String description;

    /**
     * Returns a safe string representation for debugging and logs.
     *
     * @return the department summary string
     */
    @Override
    public String toString() {
        return "Department{"
                + "id=" + getId()
                + ", name='" + name + '\''
                + ", code='" + code + '\''
                + ", description='" + description + '\''
                + '}';
    }
}
