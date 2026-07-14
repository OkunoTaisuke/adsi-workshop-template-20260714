# Unit 1: 勤怠打刻

## 概要

出勤・退勤・休憩の打刻機能と、勤怠一覧・打刻修正機能を実装する。

## 依存

- unit_0_foundation（Employee Entity, 認証基盤）

## ユーザーストーリー

- 社員として、出勤ボタンを押して出勤を記録したい
- 社員として、退勤ボタンを押して退勤を記録したい
- 社員として、休憩開始/終了を記録したい
- 社員として、自分の月別勤怠一覧を確認したい
- 社員として、打刻を修正したい（承認不要）
- 管理者として、全社員の勤怠一覧を確認したい
- 管理者として、社員名・日付で絞り込みたい

## スコープ

### Backend

- Flyway: `V2__create_attendance_records.sql`, `V3__create_break_records.sql`, `V4__create_attendance_revisions.sql`
- Entity: AttendanceRecord, BreakRecord, AttendanceRevision
- Repository: AttendanceRecordRepository, BreakRecordRepository, AttendanceRevisionRepository
- Service: AttendanceService（interface + impl）
- Controller: AttendanceController
- Value Object: WorkDuration（勤務時間計算）

### Frontend

- ダッシュボード画面（`/`）: 打刻ボタン + 今日の状態
- 勤怠一覧画面（`/attendance`）: 月別表示
- 打刻修正画面（`/attendance/[id]/edit`）
- 管理者勤怠一覧画面（`/admin/attendance`）

## テーブル

### attendance_records

```sql
CREATE TABLE attendance_records (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    date DATE NOT NULL,
    clock_in TIME,
    clock_out TIME,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(employee_id, date)
);
```

### break_records

```sql
CREATE TABLE break_records (
    id BIGSERIAL PRIMARY KEY,
    attendance_record_id BIGINT NOT NULL REFERENCES attendance_records(id),
    break_start TIME NOT NULL,
    break_end TIME
);
```

### attendance_revisions

```sql
CREATE TABLE attendance_revisions (
    id BIGSERIAL PRIMARY KEY,
    attendance_record_id BIGINT NOT NULL REFERENCES attendance_records(id),
    field_name VARCHAR(50) NOT NULL,
    old_value VARCHAR(255),
    new_value VARCHAR(255),
    revised_at TIMESTAMP NOT NULL DEFAULT NOW(),
    revised_by BIGINT NOT NULL REFERENCES employees(id)
);
```

## API エンドポイント

| Method | Path | 説明 | ロール |
|--------|------|------|--------|
| POST | /api/attendance/clock-in | 出勤打刻 | 全員 |
| POST | /api/attendance/clock-out | 退勤打刻 | 全員 |
| POST | /api/attendance/break-start | 休憩開始 | 全員 |
| POST | /api/attendance/break-end | 休憩終了 | 全員 |
| GET | /api/attendance/my?year&month | 自分の勤怠一覧 | 全員 |
| GET | /api/attendance/all?year&month&employeeName | 全社員勤怠一覧 | ADMIN |
| PUT | /api/attendance/{id} | 打刻修正 | 本人のみ |

## ビジネスルール

- 同日二重出勤不可（既に clock_in がある日に再度 clock-in → 409）
- 出勤前に退勤不可（clock_in が null → 400）
- 休憩は出勤中のみ記録可能
- 打刻修正時は AttendanceRevision に変更履歴を保存
- 勤務時間 = (clockOut - clockIn) - 休憩合計
- 残業 = 勤務時間 - 480分（8時間）

## テスト観点

- AttendanceService: 出勤打刻が正しく記録される
- AttendanceService: 同日二重出勤で例外
- AttendanceService: 退勤で勤務時間が計算される
- AttendanceService: 休憩が正しく記録・計算される
- AttendanceService: 打刻修正で履歴が保存される
- AttendanceController: 各エンドポイントの正常系・異常系
- WorkDuration: 勤務時間・残業時間の計算

## 完了基準

- [ ] 全テスト GREEN
- [ ] 打刻 → 一覧確認 → 修正の E2E 動作
- [ ] 管理者画面で全社員の勤怠確認
- [ ] テストカバレッジ 80% 以上
