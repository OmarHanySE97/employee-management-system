# Employee Management System

Backend service for managing employees and departments.

## Overview

Employee Management System is a layered REST API built with Spring Boot for managing employees, departments, and application users. It includes JWT-based authentication, role-based authorization, structured validation, global error handling, observability endpoints, database migrations, Docker support, and automated tests.

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA
- PostgreSQL
- Spring Security
- JWT
- Flyway
- MapStruct
- Lombok
- Swagger / OpenAPI
- Spring Boot Actuator
- Docker and Docker Compose
- JUnit 5
- Mockito
- Testcontainers PostgreSQL

## Features

- Employee CRUD
- Department CRUD
- JWT authentication
- Role-based authorization
- Pagination and sorting
- Employee filtering
- Bulk employee creation
- Bulk employee status update
- Validation
- Global error handling
- Correlation ID support
- Structured logging
- Flyway migrations
- PostgreSQL persistence
- Swagger documentation
- Actuator endpoints
- Docker support
- Unit and integration tests

## Architecture

The application follows a clean layered structure:

`Controller -> Service -> Repository -> Database`

- Controllers expose REST endpoints and handle request/response flow.
- Services contain business logic, validations, transaction boundaries, and logging.
- Repositories handle persistence using Spring Data JPA and specifications.
- PostgreSQL stores application data with constraints, indexes, and foreign keys.

DTOs are used for all API contracts so entities are never exposed directly. MapStruct mappers convert between entities and response DTOs to keep controller responses consistent and safe.

## Security Model

The API uses stateless JWT authentication. Users authenticate through the login endpoint, receive a bearer token, and send it in the `Authorization` header for protected endpoints.

Roles:

- `ADMIN`
- `HR`
- `VIEWER`

Public endpoints:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/actuator/health`

All other endpoints require authentication.

## Roles and Permissions

| Capability | ADMIN | HR | VIEWER |
| --- | --- | --- | --- |
| Register / Login | Yes | Yes | Yes |
| View departments | Yes | Yes | Yes |
| Create departments | Yes | Yes | No |
| Update departments | Yes | Yes | No |
| Delete departments | Yes | No | No |
| View employees | Yes | Yes | Yes |
| Create employees | Yes | Yes | No |
| Update employees | Yes | Yes | No |
| Change employee status | Yes | Yes | No |
| Soft delete employees | Yes | Yes | No |
| Bulk employee create | Yes | Yes | No |
| Bulk employee status update | Yes | Yes | No |

## Database Design Summary

The system is centered around three main tables:

- `users`: stores application users, BCrypt-hashed passwords, and assigned roles.
- `departments`: stores department metadata such as name, code, and description.
- `employees`: stores employee profile and employment details, linked to a department through a foreign key.

The schema includes uniqueness constraints, foreign keys, check constraints, and indexes to support integrity and query performance.

## How to Run with Docker Compose

```bash
docker compose up --build
```

This starts:

- PostgreSQL on `localhost:5432`
- The Spring Boot app on `localhost:8080`

Flyway migrations run automatically on startup inside Docker.
The Docker setup runs the application with the `dev` profile.

## How to Run Locally

Make sure PostgreSQL is running and accessible, then start the app with:

```bash
mvn spring-boot:run
```

The application defaults to the `local` profile, which uses:

- `jdbc:postgresql://localhost:5432/employee_management`
- username `postgres`
- password `12345`

To run explicitly with the `dev` profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Environment Variables

The application can be configured with these environment variables:

| Variable | Description | Example |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/employee_management` |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password | `12345` |
| `JWT_SECRET` | Secret used to sign JWT tokens | `strong-dev-secret-change-in-production` |
| `JWT_EXPIRATION` | JWT expiration in milliseconds | `86400000` |

Profile summary:

- `local` is the default profile and uses local PostgreSQL defaults from `application-local.properties`.
- `dev` is intended for Docker or shared environments and reads datasource and JWT settings from environment variables.

## Swagger URL

Swagger UI:

`http://localhost:8080/swagger-ui/index.html`

## Actuator Endpoints

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`

`/actuator/health` is public. Other actuator endpoints require authentication when security is enabled.

## Default Users

These users are seeded by Flyway for development and testing:

| Role | Username | Password |
| --- | --- | --- |
| `ADMIN` | `admin` | `Admin@123` |
| `HR` | `hr` | `Hr@123` |
| `VIEWER` | `viewer` | `Viewer@123` |

## Main API Endpoints

### Auth

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`

### Departments

- `POST /api/v1/departments`
- `GET /api/v1/departments`
- `GET /api/v1/departments/{id}`
- `PUT /api/v1/departments/{id}`
- `DELETE /api/v1/departments/{id}`

### Employees

- `POST /api/v1/employees`
- `GET /api/v1/employees`
- `GET /api/v1/employees/{id}`
- `PUT /api/v1/employees/{id}`
- `PATCH /api/v1/employees/{id}/status`
- `DELETE /api/v1/employees/{id}`

### Bulk Operations

- `POST /api/v1/employees/bulk`
- `PATCH /api/v1/employees/bulk/status`

## Example API Requests

### Login

```bash
curl --request POST 'http://localhost:8080/api/v1/auth/login' \
  --header 'Content-Type: application/json' \
  --data '{
    "username": "admin",
    "password": "Admin@123"
  }'
```

### Create Department

```bash
curl --request POST 'http://localhost:8080/api/v1/departments' \
  --header 'Authorization: Bearer <token>' \
  --header 'Content-Type: application/json' \
  --data '{
    "name": "Product",
    "code": "PRD",
    "description": "Product department"
  }'
```

### Create Employee

```bash
curl --request POST 'http://localhost:8080/api/v1/employees' \
  --header 'Authorization: Bearer <token>' \
  --header 'Content-Type: application/json' \
  --data '{
    "firstName": "Omar",
    "lastName": "Hany",
    "email": "omar@example.com",
    "phoneNumber": "+201000000000",
    "hireDate": "2024-01-01",
    "salary": 15000,
    "status": "ACTIVE",
    "jobTitle": "Backend Developer",
    "departmentId": 1
  }'
```

### Filter Employees

```bash
curl --request GET 'http://localhost:8080/api/v1/employees?status=ACTIVE&departmentId=1&keyword=omar&minSalary=5000&maxSalary=20000&hireDateFrom=2024-01-01&hireDateTo=2025-12-31' \
  --header 'Authorization: Bearer <token>'
```

### Bulk Create Employees

```bash
curl --request POST 'http://localhost:8080/api/v1/employees/bulk' \
  --header 'Authorization: Bearer <token>' \
  --header 'Content-Type: application/json' \
  --data '[
    {
      "firstName": "Omar",
      "lastName": "Hany",
      "email": "omar@example.com",
      "phoneNumber": "+201000000000",
      "hireDate": "2024-01-01",
      "salary": 15000,
      "status": "ACTIVE",
      "jobTitle": "Backend Developer",
      "departmentId": 1
    }
  ]'
```

### Bulk Employee Status Update

```bash
curl --request PATCH 'http://localhost:8080/api/v1/employees/bulk/status' \
  --header 'Authorization: Bearer <token>' \
  --header 'Content-Type: application/json' \
  --data '{
    "employeeIds": [1, 2, 3],
    "status": "INACTIVE"
  }'
```

## Testing Instructions

Run the full test suite with:

```bash
mvn clean test
```

The project includes unit tests with Mockito and integration tests with Spring Boot, MockMvc, and PostgreSQL Testcontainers.

## Design Decisions

- DTOs are used instead of exposing entities directly to keep API contracts stable and secure.
- JWT is used for stateless authentication and clean API integration.
- Flyway manages database versioning and repeatable environment setup.
- Pagination and sorting are used for scalable list endpoints.
- Global exception handling provides consistent error responses.
- Employee deletion is implemented as a soft delete by moving status to `TERMINATED`.
- Database indexes support common filters and lookup paths.
- Correlation IDs improve traceability across requests and logs.
- Bulk operations support partial success so one invalid record does not fail the whole batch.

## Production-Readiness Notes

- Request validation is enforced at the DTO layer.
- Database constraints protect core business data integrity.
- Role-based access control protects sensitive operations.
- Correlation-aware logging improves troubleshooting.
- Health and metrics endpoints support operational visibility.
- Unit and integration tests help prevent regressions.
- Docker support simplifies consistent local and deployment environments.

## Future Improvements

- Refresh token support
- Dedicated audit log table
- CSV / Excel import and export
- More advanced permission model
- Caching for high-read endpoints
- CI/CD pipeline automation
- Prometheus and Grafana monitoring
- Rate limiting and abuse protection
