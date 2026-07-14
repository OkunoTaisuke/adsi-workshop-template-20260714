CREATE TABLE attendance_records (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    date DATE NOT NULL,
    clock_in TIME,
    clock_out TIME,
    total_work_minutes INTEGER,
    total_break_minutes INTEGER,
    overtime_minutes INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(employee_id, date)
);

CREATE INDEX idx_attendance_records_employee_id ON attendance_records(employee_id);
