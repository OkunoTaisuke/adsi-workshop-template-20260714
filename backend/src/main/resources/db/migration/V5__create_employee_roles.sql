CREATE TABLE employee_roles (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    CONSTRAINT fk_employee_roles_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT uq_employee_roles UNIQUE (employee_id, role)
);

CREATE INDEX idx_employee_roles_employee_id ON employee_roles(employee_id);

-- Migrate existing role column data: EMPLOYEE -> USER, ADMIN stays ADMIN
INSERT INTO employee_roles (employee_id, role)
SELECT id, CASE role WHEN 'EMPLOYEE' THEN 'USER' ELSE role END
FROM employees;
