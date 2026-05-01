INSERT INTO departments (name, code, description, created_at, updated_at)
VALUES
    ('Engineering', 'ENG', 'Engineering department', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Human Resources', 'HR', 'Human Resources department', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Finance', 'FIN', 'Finance department', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Operations', 'OPS', 'Operations department', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (username, password, role, created_at, updated_at)
VALUES
    ('admin', '$2a$10$GExjezD1njQgRoArr/PZN.LUwdbPcakHVypvfhrHOIV0MWVOifj7G', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('hr', '$2a$10$JISk9j5l4rCqFgi9fryIZOTgYbuTD8Bj.vzmM3sepBb/9wmtTq5ba', 'HR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('viewer', '$2a$10$ZBGEWvR08r8FvNnsi2XMcuVZ03xukv0o9dJKDXPu8X58sVB5mAKpi', 'VIEWER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
