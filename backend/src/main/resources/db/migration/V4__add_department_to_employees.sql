ALTER TABLE employees ADD COLUMN department_id BIGINT REFERENCES departments(id);

UPDATE employees SET department_id = 1;
