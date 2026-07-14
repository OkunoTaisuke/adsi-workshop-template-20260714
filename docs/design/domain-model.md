# ドメインモデル設計

## Entity

### Employee（社員）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | 自動採番 PK |
| email | String | メールアドレス（ログインID・一意） |
| password | String | BCrypt ハッシュ |
| name | String | 氏名 |
| role | Role (enum) | EMPLOYEE / ADMIN |
| departmentId | Long | FK → Department（nullable） |
| createdAt | LocalDateTime | 作成日時 |
| updatedAt | LocalDateTime | 更新日時 |
| version | Long | 楽観ロック |

### Department（部署）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | 自動採番 PK |
| name | String | 部署名（UNIQUE） |
| createdAt | LocalDateTime | 作成日時 |

### AttendanceRecord（勤怠記録）

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
- 同一社員・同一日付で一意（UNIQUE制約）
- 日跨ぎ: `clockOut < clockIn` の場合、翌日退勤とみなす
- 勤務時間 = (clockOut - clockIn) - 休憩合計（日跨ぎ考慮）
- 残業 = max(0, 勤務時間 - 480分)
- 打刻無効化: clockIn / clockOut を null にセット可能（レコード自体は削除しない）

### BreakRecord（休憩記録）

1日に複数回可能。

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | 自動採番 PK |
| attendanceRecordId | Long | FK → AttendanceRecord |
| breakStart | LocalTime | 休憩開始 |
| breakEnd | LocalTime | 休憩終了 |

### AttendanceRevision（打刻修正履歴）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | 自動採番 PK |
| attendanceRecordId | Long | FK → AttendanceRecord |
| fieldName | String | 修正対象フィールド名 |
| oldValue | String | 修正前の値 |
| newValue | String | 修正後の値 |
| revisedAt | LocalDateTime | 修正日時 |
| revisedBy | Long | FK → Employee（修正者） |

### LeaveRequest（休暇申請）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | 自動採番 PK |
| employeeId | Long | FK → Employee |
| leaveType | LeaveType (enum) | PAID / SICK / OTHER |
| startDate | LocalDate | 開始日 |
| endDate | LocalDate | 終了日 |
| reason | String | 申請理由（任意） |
| status | LeaveStatus (enum) | PENDING / APPROVED / REJECTED |
| approvedBy | Long | FK → Employee（承認者、nullable） |
| approvedAt | LocalDateTime | 承認/却下日時（nullable） |
| createdAt | LocalDateTime | 作成日時 |
| updatedAt | LocalDateTime | 更新日時 |
| version | Long | 楽観ロック |

## Value Object

### WorkDuration（勤務時間）

- totalMinutes: int（分単位。端数処理なし）
- 計算: clockOut - clockIn - 休憩合計
- 日跨ぎ対応: clockOut < clockIn → 翌日とみなし 24時間加算
- 残業: max(0, totalMinutes - 480)

## Enum（勤怠）

### AttendanceStatus（コアタイムチェック結果）

フロント表示用。API レスポンスに含める。

| 値 | 条件 |
|---|------|
| OK | 出勤 ≤ 10:00 かつ 退勤 ≥ 15:00 かつ 勤務 ≥ 480分 |
| LATE_START | 出勤 > 10:00 |
| EARLY_LEAVE | 退勤 < 15:00 |
| SHORT_HOURS | 勤務 < 480分 |

## Enum（共通）

### Role

- EMPLOYEE
- ADMIN

## Enum（休暇）

### LeaveType

- PAID（有給）
- SICK（病欠）
- OTHER（その他）

### LeaveStatus

- PENDING（申請中）
- APPROVED（承認）
- REJECTED（却下）

## ドメイン関連図

```
Department (1) ──── (N) Employee (1) ──── (N) AttendanceRecord (1) ──── (N) BreakRecord
                        │                       │
                        │                       └──── (N) AttendanceRevision
                        │
                        └──── (N) LeaveRequest
```

## Repository（interface）

- EmployeeRepository
- DepartmentRepository
- AttendanceRecordRepository
- BreakRecordRepository
- AttendanceRevisionRepository
- LeaveRequestRepository

## Service（interface）

| Service | 責務 |
|---------|------|
| AuthService | ログイン認証・トークン発行 |
| AttendanceService | 打刻（出勤/退勤/休憩）・修正・一覧取得 |
| DepartmentService | 部署一覧取得 |
| LeaveService | 休暇申請・承認/却下・一覧取得 |
| ReportService | 月次集計・CSV 生成 |
