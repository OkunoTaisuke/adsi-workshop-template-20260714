CREATE TABLE break_records (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    attendance_record_id BIGINT NOT NULL REFERENCES attendance_records(id),
    break_start TIME NOT NULL,
    break_end TIME
);

CREATE INDEX idx_break_records_attendance_id ON break_records(attendance_record_id);
