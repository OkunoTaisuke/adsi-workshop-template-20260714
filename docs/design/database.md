# DB 設計

## ER 図

```
┌──────────────┐       ┌──────────────┐       ┌─────────────────────┐       ┌──────────────────┐
│ departments  │       │  employees   │       │ attendance_records   │       │  break_records   │
├──────────────┤       ├──────────────┤       ├─────────────────────┤       ├──────────────────┤
│ id (PK)      │1────N│ id (PK)      │1────N│ id (PK)             │1────N│ id (PK)          │
│ name (UQ)    │       │ email (UQ)   │       │ employee_id (FK)     │       │ attendance_id(FK)│
│ created_at   │       │ password     │       │ date                 │       │ break_start      │
└──────────────┘       │ name         │       │ clock_in             │       │ break_end        │
                       │ role         │       │ clock_out            │       └──────────────────┘
                       │ department_id│       │ total_work_minutes   │
                       │ created_at   │       │ total_break_minutes  │
                       │ updated_at   │       │ overtime_minutes     │
                       │ version      │       │ created_at           │
                       └──────────────┘       │ updated_at           │
                              │               │ version              │
                              │               └─────────────────────┘
                              │                        │
                              │                        │1────N┌─────────────────────────┐
                              │                               │ attendance_revisions    │
                              │                               ├─────────────────────────┤
                              │                               │ id (PK)                │
                              │                               │ attendance_record_id(FK)│
                              │                               │ field_name             │
                              │                               │ old_value              │
                              │                               │ new_value              │
                              │                               │ revised_at             │
                              │                               │ revised_by (FK)        │
                              │                               └─────────────────────────┘
                              │
                              │1────N┌──────────────────┐
                                     │  leave_requests  │
                                     ├──────────────────┤
                                     │ id (PK)          │
                                     │ employee_id (FK) │
                                     │ leave_type       │
                                     │ start_date       │
                                     │ end_date         │
                                     │ reason           │
                                     │ status           │
                                     │ approved_by (FK) │
                                     │ approved_at      │
                                     │ created_at       │
                                     │ updated_at       │
                                     │ version          │
                                     └──────────────────┘
```

## テーブル定義

### departments

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGSERIAL | PK | |
| name | VARCHAR(100) | NOT NULL, UNIQUE | 部署名 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

### employees

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGSERIAL | PK | |
| email | VARCHAR(255) | NOT NULL, UNIQUE | ログインID |
| password | VARCHAR(255) | NOT NULL | BCrypt ハッシュ |
| name | VARCHAR(100) | NOT NULL | 氏名 |
| role | VARCHAR(20) | NOT NULL, DEFAULT 'EMPLOYEE' | EMPLOYEE / ADMIN |
| department_id | BIGINT | FK → departments | 所属部署（nullable） |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| version | BIGINT | NOT NULL, DEFAULT 0 | 楽観ロック |

### attendance_records

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGSERIAL | PK | |
| employee_id | BIGINT | NOT NULL, FK → employees | |
| date | DATE | NOT NULL | 勤務日 |
| clock_in | TIME | | 出勤時刻 |
| clock_out | TIME | | 退勤時刻 |
| total_work_minutes | INTEGER | | 勤務時間（分）。退勤時/修正時に計算 |
| total_break_minutes | INTEGER | | 休憩時間（分）。退勤時/修正時に計算 |
| overtime_minutes | INTEGER | | 残業時間（分）。退勤時/修正時に計算 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| version | BIGINT | NOT NULL, DEFAULT 0 | 楽観ロック |

- UNIQUE(employee_id, date)

### break_records

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGSERIAL | PK | |
| attendance_record_id | BIGINT | NOT NULL, FK → attendance_records | |
| break_start | TIME | NOT NULL | 休憩開始 |
| break_end | TIME | | 休憩終了 |

### attendance_revisions

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGSERIAL | PK | |
| attendance_record_id | BIGINT | NOT NULL, FK → attendance_records | |
| field_name | VARCHAR(50) | NOT NULL | 修正フィールド名 |
| old_value | VARCHAR(255) | | 修正前 |
| new_value | VARCHAR(255) | | 修正後 |
| revised_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| revised_by | BIGINT | NOT NULL, FK → employees | 修正者 |

### leave_requests

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGSERIAL | PK | |
| employee_id | BIGINT | NOT NULL, FK → employees | |
| leave_type | VARCHAR(20) | NOT NULL | PAID / SICK / OTHER |
| start_date | DATE | NOT NULL | |
| end_date | DATE | NOT NULL | |
| reason | VARCHAR(500) | | 申請理由 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | PENDING / APPROVED / REJECTED |
| approved_by | BIGINT | FK → employees | 承認者 |
| approved_at | TIMESTAMP | | 承認/却下日時 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| version | BIGINT | NOT NULL, DEFAULT 0 | 楽観ロック |

## インデックス

- employees: email (UNIQUE)
- employees: department_id
- attendance_records: (employee_id, date) (UNIQUE)
- attendance_records: employee_id
- break_records: attendance_record_id
- attendance_revisions: attendance_record_id
- leave_requests: employee_id
- leave_requests: status

## マイグレーション方針

- Flyway で管理する（`ddl-auto` 禁止）
- バージョン命名: `V1__create_employees.sql`, `V2__insert_sample_data.sql`, ...

### 確定済みバージョン

| バージョン | ファイル | 内容 | Unit |
|-----------|---------|------|------|
| V1 | `V1__create_employees.sql` | employees テーブル作成 | 0 |
| V2 | `V2__insert_sample_data.sql` | サンプルデータ投入 | 0 |
| V3 | `V3__create_departments.sql` | departments テーブル作成 | 1 |
| V4 | `V4__add_department_to_employees.sql` | employees に department_id 追加 | 1 |
| V5 | `V5__create_attendance_records.sql` | attendance_records（計算カラム含む） | 1 |
| V6 | `V6__create_break_records.sql` | break_records | 1 |
| V7 | `V7__create_attendance_revisions.sql` | attendance_revisions | 1 |
| V8 | `V8__create_leave_requests.sql` | leave_requests | 2 |
