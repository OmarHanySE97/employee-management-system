package com.example.employeemanagement.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing for created and last-modified timestamps.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
