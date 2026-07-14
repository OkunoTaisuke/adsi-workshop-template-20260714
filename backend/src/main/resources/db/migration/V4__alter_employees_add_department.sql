ALTER TABLE employees ADD COLUMN department_id BIGINT;

ALTER TABLE employees ADD CONSTRAINT fk_employees_department
    FOREIGN KEY (department_id) REFERENCES departments(id);

CREATE INDEX idx_employees_department_id ON employees(department_id);
