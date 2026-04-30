CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'HR', 'VIEWER'))
);

CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(1000),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_departments_name UNIQUE (name),
    CONSTRAINT uk_departments_code UNIQUE (code)
);

CREATE INDEX idx_department_code ON departments (code);

CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(30),
    hire_date DATE NOT NULL,
    salary NUMERIC(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    job_title VARCHAR(150) NOT NULL,
    department_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_employees_email UNIQUE (email),
    CONSTRAINT chk_employees_salary_positive CHECK (salary > 0),
    CONSTRAINT chk_employees_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ON_LEAVE', 'TERMINATED')),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES departments (id)
);

CREATE INDEX idx_employee_email ON employees (email);

CREATE INDEX idx_employee_status ON employees (status);

CREATE INDEX idx_employee_department_id ON employees (department_id);

CREATE INDEX idx_employee_hire_date ON employees (hire_date);
