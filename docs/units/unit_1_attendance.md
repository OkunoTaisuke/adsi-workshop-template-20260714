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
- 社員として、打刻を修正したい（当月分のみ、承認不要）
- 社員として、打刻を無効化したい（時刻を空にする）
- 管理者として、全社員の勤怠一覧を確認したい
- 管理者として、部署・社員名・月で絞り込みたい

## スコープ

### Backend

- Flyway: `V2__create_departments.sql`, `V3__add_department_to_employees.sql`, `V4__create_attendance_records.sql`, `V5__create_break_records.sql`, `V6__create_attendance_revisions.sql`
- Entity: Department, AttendanceRecord, BreakRecord, AttendanceRevision
- Repository: DepartmentRepository, AttendanceRecordRepository, BreakRecordRepository, AttendanceRevisionRepository
- Service: AttendanceService（interface + impl）
- Controller: AttendanceController, DepartmentController
- Value Object: WorkDuration（勤務時間計算・日跨ぎ対応）

### Frontend

- ダッシュボード画面（`/`）: 打刻ボタン + ポップアップ確認 + 今日の状態
- 勤怠一覧画面（`/attendance`）: 月別全日表示 + ステータスアイコン
- 打刻修正画面（`/attendance/[id]/edit`）: clockIn/Out + 休憩修正
- 管理者勤怠一覧画面（`/admin/attendance`）: 部署/月/ユーザー名で絞り込み

## テーブル

### departments（新規）

```sql
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### attendance_records

```sql
CREATE TABLE attendance_records (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    date DATE NOT NULL,
    clock_in TIME,
    clock_out TIME,
    total_work_minutes INTEGER,
    total_break_minutes INTEGER,
    overtime_minutes INTEGER,
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

| Method | Path | Request Body | 説明 | ロール |
|--------|------|-------------|------|--------|
| POST | /api/attendance/clock-in | `{ "clockIn": "09:15" }` | 出勤打刻 | 全員 |
| POST | /api/attendance/clock-out | `{ "clockOut": "18:00" }` | 退勤打刻 | 全員 |
| POST | /api/attendance/break-start | `{ "breakStart": "12:00" }` | 休憩開始 | 全員 |
| POST | /api/attendance/break-end | `{ "breakEnd": "13:00" }` | 休憩終了 | 全員 |
| GET | /api/attendance/today | — | 今日の勤怠状態 | 全員 |
| GET | /api/attendance/my?year&month | — | 自分の勤怠一覧（全日分） | 全員 |
| GET | /api/attendance/all?year&month&departmentId&employeeName | — | 全社員勤怠一覧 | ADMIN |
| PUT | /api/attendance/{id} | 修正内容 | 打刻修正（当月のみ） | 本人のみ |
| GET | /api/departments | — | 部署一覧 | 全員 |

## ビジネスルール

- 同日二重出勤不可（既に clock_in がある日に再度 clock-in → 409）
- 出勤前に退勤不可（clock_in が null → 400）
- 休憩は出勤中のみ記録可能
- 未終了の休憩がある状態での新規休憩開始 → フロントで警告ポップアップ、API では 400
- 休憩中の退勤 → フロントで警告ポップアップ、API では 400
- 打刻修正時は AttendanceRevision に変更履歴を保存
- 修正可能期間は当月分のみ
- 打刻無効化: clockIn / clockOut に null をセット可能（レコード削除はしない）
- 日跨ぎ勤務: clockOut < clockIn の場合、翌日退勤とみなす
- 勤務時間 = (clockOut - clockIn) - 休憩合計（日跨ぎ考慮）
- 残業 = max(0, 勤務時間 - 480分)
- 端数処理: 分単位でそのまま
- 退勤時/修正時に計算結果を DB に保存
- コアタイムチェック: 出勤>10:00 or 退勤<15:00 or 勤務<8h → 警告、すべてOK → グリーン

## 打刻フロー

1. ボタン押下 → ポップアップ表示（現在時刻プリセット）
2. 時刻確認/変更
3. 「送信」で API 呼び出し
4. レスポンスで画面更新

## テスト観点

- WorkDuration: 通常の勤務時間計算
- WorkDuration: 日跨ぎ勤務の計算（23:00→02:00 = 3時間）
- WorkDuration: 休憩時間を差し引いた計算
- WorkDuration: 残業時間の計算（8時間超過分）
- AttendanceService: 出勤打刻が正しく記録される
- AttendanceService: 同日二重出勤で例外
- AttendanceService: 退勤で勤務時間が計算・保存される
- AttendanceService: 未終了休憩がある状態で退勤 → 400
- AttendanceService: 休憩が正しく記録される
- AttendanceService: 打刻修正で履歴が保存される
- AttendanceService: 当月以外の修正で 400
- AttendanceService: clockIn/clockOut を null にして無効化
- AttendanceService: コアタイムチェックの判定
- AttendanceController: 各エンドポイントの正常系・異常系
- 月別一覧: 全日分がレスポンスに含まれる（レコードなし日は null）
- 管理者一覧: 部署/ユーザー名/月での絞り込み

## 完了基準

- [ ] 全テスト GREEN
- [ ] 打刻（ポップアップ確認）→ 一覧確認 → 修正の E2E 動作
- [ ] 日跨ぎ勤務の正しい計算
- [ ] コアタイムチェック表示（グリーン/警告）
- [ ] 管理者画面で部署/月/ユーザー名絞り込み
- [ ] テストカバレッジ 80% 以上
