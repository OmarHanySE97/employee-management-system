package com.example.employeemanagement.repository;

import com.example.employeemanagement.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for application users used by authentication and registration flows.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by username.
     *
     * @param username the username to search for
     * @return the matching user if present
     */
    Optional<User> findByUsername(String username);

    /**
     * Checks whether a username is already in use.
     *
     * @param username the username to check
     * @return {@code true} when the username already exists
     */
    boolean existsByUsername(String username);
}
