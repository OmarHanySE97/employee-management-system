package com.example.employeemanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base JPA entity that provides identifiers and auditing timestamps.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Compares entities by persistent identifier while remaining safe for Hibernate proxies.
     *
     * @param object the other object to compare with
     * @return {@code true} when both entities have the same non-null identifier
     */
    @Override
    public final boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) {
            return false;
        }

        BaseAuditableEntity that = (BaseAuditableEntity) object;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    /**
     * Returns a stable hash code implementation compatible with identifier-based equality.
     *
     * @return the hash code for this entity type
     */
    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
