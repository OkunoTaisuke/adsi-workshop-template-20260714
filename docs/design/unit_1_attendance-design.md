# Unit 1: 勤怠打刻 — 確定版設計

> 要求: `docs/units/unit_1_attendance.md`
> QA: `docs/working/design/unit_1_attendance-qa.md`, `unit_1_attendance-qa-followup.md`

---

## 1. ドメインモデル

### Entity

#### AttendanceRecord（勤怠記録）

1日1社員につき1レコード。

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | 自動採番 PK |
| employeeId | Long | FK → Employee |
| date | LocalDate | 勤務日（出勤日基準） |
| clockIn | LocalTime | 出勤時刻（nullable: 無効化時） |
| clockOut | LocalTime | 退勤時刻（nullable） |
| totalWorkMinutes | Integer | 勤務時間（分）。退勤時/修正時に計算。nullable |
| totalBreakMinutes | Integer | 休憩時間（分）。退勤時/修正時に計算。nullable |
| overtimeMinutes | Integer | 残業時間（分）。退勤時/修正時に計算。nullable |
| createdAt | LocalDateTime | 作成日時 |
| updatedAt | LocalDateTime | 更新日時 |
| version | Long | 楽観ロック |

ビジネスルール:
- UNIQUE(employee_id, date)
- 日跨ぎ: `clockOut < clockIn` の場合、翌日退勤とみなす
- 勤務時間 = (clockOut - clockIn) - 休憩合計（日跨ぎ考慮）
- 残業 = 勤務時間 - 480分。負の場合は 0
- 打刻無効化: clockIn / clockOut を null にセット可能（レコード自体は削除しない）

#### BreakRecord（休憩記録）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | 自動採番 PK |
| attendanceRecordId | Long | FK → AttendanceRecord |
| breakStart | LocalTime | 休憩開始 |
| breakEnd | LocalTime | 休憩終了（nullable: 未終了） |

ビジネスルール:
- 未終了の休憩がある場合、新規休憩開始時にフロントでポップアップ警告
- 休憩中に退勤しようとした場合もフロントでポップアップ警告
- 休憩時刻の修正も可能

#### AttendanceRevision（修正履歴）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | 自動採番 PK |
| attendanceRecordId | Long | FK → AttendanceRecord |
| fieldName | String | 修正対象フィールド名（clockIn, clockOut, breakStart, breakEnd） |
| oldValue | String | 修正前の値 |
| newValue | String | 修正後の値 |
| revisedAt | LocalDateTime | 修正日時 |
| revisedBy | Long | FK → Employee |

制約:
- 当月分のみ修正可能（前月以前は修正不可）

#### Department（部署）— 新規追加

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | 自動採番 PK |
| name | String | 部署名 |
| createdAt | LocalDateTime | 作成日時 |

### Value Object

#### WorkDuration（勤務時間）

- totalMinutes: int（分単位。端数処理なし）
- 計算: clockOut - clockIn - 休憩合計
- 日跨ぎ対応: clockOut < clockIn → 翌日とみなし 24時間加算
- 残業: max(0, totalMinutes - 480)

### Enum

#### AttendanceStatus（コアタイムチェック結果）

フロント表示用。API レスポンスに含める。

| 値 | 条件 | 表示 |
|---|------|------|
| OK | 出勤 ≤ 10:00 かつ 退勤 ≥ 15:00 かつ 勤務 ≥ 480分 | グリーンチェック |
| LATE_START | 出勤 > 10:00 | 警告マーク |
| EARLY_LEAVE | 退勤 < 15:00 | 警告マーク |
| SHORT_HOURS | 勤務 < 480分 | 警告マーク |

複数条件に該当する場合は最も重いものを返す（優先順位: SHORT_HOURS > LATE_START > EARLY_LEAVE）。
未退勤の場合は null。

---

## 2. DB 設計

### テーブル変更

#### departments（新規）

```sql
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

#### employees（変更: department_id 追加）

```sql
ALTER TABLE employees ADD COLUMN department_id BIGINT REFERENCES departments(id);
```

#### attendance_records（変更: 計算結果カラム追加）

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

#### break_records（変更なし）

```sql
CREATE TABLE break_records (
    id BIGSERIAL PRIMARY KEY,
    attendance_record_id BIGINT NOT NULL REFERENCES attendance_records(id),
    break_start TIME NOT NULL,
    break_end TIME
);
```

#### attendance_revisions（変更なし）

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

### Flyway マイグレーション

| バージョン | ファイル | 内容 |
|-----------|---------|------|
| V2 | `V2__create_departments.sql` | departments テーブル作成 |
| V3 | `V3__add_department_to_employees.sql` | employees に department_id 追加 |
| V4 | `V4__create_attendance_records.sql` | attendance_records（計算カラム含む） |
| V5 | `V5__create_break_records.sql` | break_records |
| V6 | `V6__create_attendance_revisions.sql` | attendance_revisions |

### インデックス

- attendance_records: (employee_id, date) UNIQUE
- attendance_records: employee_id
- break_records: attendance_record_id
- attendance_revisions: attendance_record_id
- employees: department_id

---

## 3. API 設計

### 打刻 API（リクエストボディ付き）

| Method | Path | Request Body | 説明 | ロール |
|--------|------|-------------|------|--------|
| POST | /api/attendance/clock-in | `{ "clockIn": "09:15" }` | 出勤打刻 | 全員 |
| POST | /api/attendance/clock-out | `{ "clockOut": "18:00" }` | 退勤打刻 | 全員 |
| POST | /api/attendance/break-start | `{ "breakStart": "12:00" }` | 休憩開始 | 全員 |
| POST | /api/attendance/break-end | `{ "breakEnd": "13:00" }` | 休憩終了 | 全員 |
| GET | /api/attendance/today | — | 今日の勤怠状態取得 | 全員 |
| GET | /api/attendance/my?year&month | — | 自分の月別一覧（全日分） | 全員 |
| GET | /api/attendance/all?year&month&departmentId&employeeName | — | 全社員一覧 | ADMIN |
| PUT | /api/attendance/{id} | 修正内容 | 打刻修正（当月分のみ） | 本人 |
| GET | /api/departments | — | 部署一覧取得 | 全員 |

### リクエスト/レスポンス DTO

#### ClockInRequest

```json
{ "clockIn": "09:15" }
```

#### ClockOutRequest

```json
{ "clockOut": "18:00" }
```

#### BreakStartRequest

```json
{ "breakStart": "12:00" }
```

#### BreakEndRequest

```json
{ "breakEnd": "13:00" }
```

#### AttendanceUpdateRequest（修正）

```json
{
  "clockIn": "09:00",
  "clockOut": "18:00",
  "breaks": [
    { "id": 1, "breakStart": "12:00", "breakEnd": "13:00" },
    { "breakStart": "15:00", "breakEnd": "15:15" }
  ]
}
```

- clockIn / clockOut に null を送信すると無効化
- breaks 配列で休憩の追加・修正・削除を表現（id なし = 新規、既存 ID が含まれない = 削除）

#### AttendanceResponse（打刻レスポンス）

```json
{
  "id": 1,
  "date": "2026-07-14",
  "clockIn": "09:15",
  "clockOut": "18:00",
  "breaks": [
    { "id": 1, "breakStart": "12:00", "breakEnd": "13:00" }
  ],
  "totalWorkMinutes": 480,
  "totalBreakMinutes": 60,
  "overtimeMinutes": 0,
  "status": "OK"
}
```

#### AttendanceDetailResponse（一覧レスポンス）

```json
{
  "id": 1,
  "employeeId": 10,
  "employeeName": "田中太郎",
  "departmentName": "開発部",
  "date": "2026-07-14",
  "clockIn": "09:15",
  "clockOut": "18:00",
  "breaks": [...],
  "totalWorkMinutes": 480,
  "totalBreakMinutes": 60,
  "overtimeMinutes": 0,
  "status": "OK"
}
```

月別一覧 API は全日分を返す。レコードがない日は以下のように返す:

```json
{
  "id": null,
  "date": "2026-07-15",
  "clockIn": null,
  "clockOut": null,
  "breaks": [],
  "totalWorkMinutes": null,
  "totalBreakMinutes": null,
  "overtimeMinutes": null,
  "status": null
}
```

### エラーレスポンス

```json
{
  "error": "ALREADY_CLOCKED_IN",
  "message": "本日は既に出勤打刻済みです"
}
```

| ステータス | エラーコード | 条件 |
|-----------|------------|------|
| 409 | ALREADY_CLOCKED_IN | 同日二重出勤 |
| 400 | NOT_CLOCKED_IN | 出勤前に退勤/休憩 |
| 400 | BREAK_NOT_ENDED | 未終了の休憩あり（API レベル。フロントのポップアップをすり抜けた場合） |
| 400 | NOT_CURRENT_MONTH | 当月以外の修正 |
| 403 | FORBIDDEN | 他人の記録修正 / 管理者以外が全社員一覧参照 |
| 404 | NOT_FOUND | 存在しないレコード |

---

## 4. フロントエンド設計

### 画面一覧

| 画面 | パス | 説明 |
|------|------|------|
| ダッシュボード | `/` | 打刻ボタン + 今日の状態。ポップアップで時刻確認 |
| 勤怠一覧 | `/attendance` | 月別全日表示。土日色分け。ステータスアイコン |
| 打刻修正 | `/attendance/[id]/edit` | clock_in/out + 休憩の修正。当月のみ |
| 管理者勤怠一覧 | `/admin/attendance` | 部署/月選択 + ユーザー名入力で絞り込み |

### ダッシュボード — 打刻フロー

1. ユーザーが「出勤」「退勤」「休憩開始」「休憩終了」ボタンを押す
2. ポップアップが表示される（現在時刻がプリセット）
3. 時刻を確認/変更
4. 「送信」ボタンで API を呼び出す
5. レスポンスで今日の状態を更新

#### ポップアップ警告

- 未終了の休憩がある状態で「休憩開始」→ 「未終了の休憩があります」と表示
- 休憩中に「退勤」→ 「休憩が終了していません」と表示

### 勤怠一覧

- 月別全日表示（その月の 1日〜末日）
- 曜日の色分け: 土日 = 強調色、平日 = 通常
- ステータスカラム: グリーンチェック or 警告マーク
- 各行クリックで修正画面へ遷移（当月のみ）

### 管理者勤怠一覧

- 部署ドロップダウン（`GET /api/departments`）
- 月選択
- ユーザー名テキスト入力（部分一致）
- 1ページで全件表示（ページネーションなし）

---

## 5. パッケージ構成

```
com.example.attendance
├── application
│   ├── dto/
│   │   ├── ClockInRequest.java
│   │   ├── ClockOutRequest.java
│   │   ├── BreakStartRequest.java
│   │   ├── BreakEndRequest.java
│   │   ├── AttendanceUpdateRequest.java
│   │   ├── AttendanceResponse.java
│   │   ├── AttendanceDetailResponse.java
│   │   ├── BreakResponse.java
│   │   └── DepartmentResponse.java
│   └── service/
│       ├── AttendanceService.java (interface)
│       └── AttendanceServiceImpl.java
├── domain
│   ├── model/
│   │   ├── AttendanceRecord.java
│   │   ├── BreakRecord.java
│   │   ├── AttendanceRevision.java
│   │   ├── Department.java
│   │   └── WorkDuration.java (Value Object)
│   └── repository/
│       ├── AttendanceRecordRepository.java
│       ├── BreakRecordRepository.java
│       ├── AttendanceRevisionRepository.java
│       └── DepartmentRepository.java
├── infrastructure
│   └── exception/
│       └── (既存 GlobalExceptionHandler を拡張)
└── presentation
    └── controller/
        ├── AttendanceController.java
        └── DepartmentController.java
```

---

## 6. 設計判断のサマリ

| 項目 | 決定 |
|------|------|
| 日跨ぎ | TIME 型のまま。clockOut < clockIn → 翌日とみなす |
| 打刻時刻 | クライアントからリクエストボディで送信（ポップアップ確認） |
| 休憩デフォルト | 自動設定なし |
| 休憩制約 | フロントのポップアップで警告。API でも 400 で拒否 |
| 打刻無効化 | 修正 API で null 送信。レコード削除はしない |
| 修正対象 | clockIn, clockOut, breakStart, breakEnd すべて |
| 修正可能期間 | 当月分のみ |
| 勤務時間保存 | DB に保存（退勤時/修正時に再計算） |
| 端数処理 | 分単位でそのまま |
| コアタイム | チェック結果を status として返す。警告/グリーン表示 |
| 一覧表示 | 月の全日分を返す（レコードなしの日は null） |
| 管理者絞り込み | 部署 + 月 + ユーザー名。ページネーションなし |
| 所属（部署） | departments テーブル新設、employees に FK 追加 |
| リアルタイム性 | ページロード時に1回取得のみ |
| 曜日表示 | 土日と平日の色分け（祝日は不要） |
