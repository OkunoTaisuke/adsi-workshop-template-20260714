CREATE TABLE attendance_revisions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    attendance_record_id BIGINT NOT NULL REFERENCES attendance_records(id),
    field_name VARCHAR(50) NOT NULL,
    old_value VARCHAR(255),
    new_value VARCHAR(255),
    revised_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revised_by BIGINT NOT NULL REFERENCES employees(id)
);

CREATE INDEX idx_attendance_revisions_attendance_id ON attendance_revisions(attendance_record_id);
