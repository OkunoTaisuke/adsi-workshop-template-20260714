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
| createdAt | LocalDateTime | 作成日時 |
| updatedAt | LocalDateTime | 更新日時 |
| version | Long | 楽観ロック |

### AttendanceRecord（勤怠記録）

1日1社員につき1レコード。

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | 自動採番 PK |
| employeeId | Long | FK → Employee |
| date | LocalDate | 勤務日 |
| clockIn | LocalTime | 出勤時刻 |
| clockOut | LocalTime | 退勤時刻 |
| createdAt | LocalDateTime | 作成日時 |
| updatedAt | LocalDateTime | 更新日時 |
| version | Long | 楽観ロック |

ビジネスルール:
- 同一社員・同一日付で一意（UNIQUE制約）
- clockIn が null でない限り clockOut は打刻可能
- 勤務時間 = clockOut - clockIn - 休憩合計

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

- totalMinutes: int
- 計算: clockOut - clockIn - 休憩合計
- 残業: totalMinutes - 480（8時間 = 480分）を超えた分

## Enum

### Role

- EMPLOYEE
- ADMIN

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
Employee (1) ──── (N) AttendanceRecord (1) ──── (N) BreakRecord
    │                       │
    │                       └──── (N) AttendanceRevision
    │
    └──── (N) LeaveRequest
```

## Repository（interface）

- EmployeeRepository
- AttendanceRecordRepository
- BreakRecordRepository
- AttendanceRevisionRepository
- LeaveRequestRepository

## Service（interface）

| Service | 責務 |
|---------|------|
| AuthService | ログイン認証・トークン発行 |
| AttendanceService | 打刻（出勤/退勤/休憩）・修正・一覧取得 |
| LeaveService | 休暇申請・承認/却下・一覧取得 |
| ReportService | 月次集計・CSV 生成 |
