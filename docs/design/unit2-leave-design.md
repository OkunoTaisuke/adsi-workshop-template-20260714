# Unit 2: 休暇申請 — 基本設計

## 概要

組織管理（部署・社員登録）＋ 休暇申請・承認機能の設計。
既存の Employee テーブルを拡張し、部署・複数ロールを導入する。
承認権限は「申請者と同一部署の MANAGER ロール社員」に付与する（manager_id は使わない）。

---

## ドメインモデル

### 新規 Entity

#### Department（部署）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | 自動採番 PK |
| name | String | 部署名（一意） |
| createdAt | LocalDateTime | 作成日時 |
| updatedAt | LocalDateTime | 更新日時 |
| version | Long | 楽観ロック |

#### EmployeeRole（社員ロール）

社員とロールの多対多を中間テーブルで表現する。

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | 自動採番 PK |
| employeeId | Long | FK → Employee |
| role | Role (enum) | ADMIN / MANAGER / USER |

#### LeaveRequest（休暇申請）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long | 自動採番 PK |
| employeeId | Long | FK → Employee（申請者） |
| leaveType | LeaveType (enum) | PAID |
| leavePeriodType | LeavePeriodType (enum) | FULL_DAY / AM_ONLY / PM_ONLY |
| startDate | LocalDate | 開始日 |
| endDate | LocalDate | 終了日 |
| reason | String | 申請理由（任意、500文字以内） |
| status | LeaveStatus (enum) | PENDING / APPROVED / REJECTED / CANCELLED |
| approvedBy | Long | FK → Employee（承認者、nullable） |
| approvedAt | LocalDateTime | 承認/却下日時（nullable） |
| rejectionReason | String | 却下理由（任意、nullable） |
| createdAt | LocalDateTime | 作成日時 |
| updatedAt | LocalDateTime | 更新日時 |
| version | Long | 楽観ロック |

### 既存 Entity の変更

#### Employee（拡張）

| 追加フィールド | 型 | 説明 |
|--------------|-----|------|
| departmentId | Long | FK → Department（所属部署） |

- `manager_id` は導入しない。承認者は「同一部署の MANAGER ロール社員」で決定する
- 既存の `role` カラム（単一値）は廃止し、`employee_roles` テーブルに移行
- 後方互換: 移行マイグレーションで既存データを変換（`EMPLOYEE` → `USER` にマッピング）

### Enum

| Enum | 値 | 画面表示 |
|------|-----|---------|
| Role | ADMIN / MANAGER / USER | システム管理者 / 上司 / 一般ユーザー |
| LeaveType | PAID | 有給休暇 |
| LeavePeriodType | FULL_DAY / AM_ONLY / PM_ONLY | 全日 / 午前休 / 午後休 |
| LeaveStatus | PENDING / APPROVED / REJECTED / CANCELLED | 申請中 / 承認 / 却下 / キャンセル |

### Value Object

なし（シンプルなフィールドで十分）

---

## ドメイン関連図

```
Department (1) ──── (N) Employee (1) ──── (N) EmployeeRole
                            │
                            └──── (N) LeaveRequest
                                          │
                                          └── approvedBy → Employee
```

- 承認者の決定: 申請者と同じ department_id を持ち、employee_roles に MANAGER を持つ社員

---

## DB 設計

### 新規テーブル

#### departments

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGINT GENERATED ALWAYS AS IDENTITY | PK | |
| name | VARCHAR(100) | NOT NULL, UNIQUE | 部署名 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| version | BIGINT | NOT NULL, DEFAULT 0 | 楽観ロック |

#### employee_roles

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGINT GENERATED ALWAYS AS IDENTITY | PK | |
| employee_id | BIGINT | NOT NULL, FK → employees | |
| role | VARCHAR(20) | NOT NULL | ADMIN / MANAGER / USER |

- UNIQUE(employee_id, role)

#### leave_requests

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGINT GENERATED ALWAYS AS IDENTITY | PK | |
| employee_id | BIGINT | NOT NULL, FK → employees | 申請者 |
| leave_type | VARCHAR(20) | NOT NULL | PAID |
| leave_period_type | VARCHAR(20) | NOT NULL | FULL_DAY / AM_ONLY / PM_ONLY |
| start_date | DATE | NOT NULL | |
| end_date | DATE | NOT NULL | |
| reason | VARCHAR(500) | | 申請理由 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | |
| approved_by | BIGINT | FK → employees | 承認者 |
| approved_at | TIMESTAMP | | 承認/却下日時 |
| rejection_reason | VARCHAR(500) | | 却下理由 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| version | BIGINT | NOT NULL, DEFAULT 0 | 楽観ロック |

### 既存テーブルの変更

#### employees（ALTER）

| 追加カラム | 型 | 制約 | 説明 |
|-----------|-----|------|------|
| department_id | BIGINT | FK → departments, nullable（移行期間） | 所属部署 |

- `manager_id` は追加しない（承認者は同一部署の MANAGER で判定）
- 既存の `role` カラムは残し（後方互換）、新規ロール判定は `employee_roles` を使う

### インデックス

- departments: name (UNIQUE)
- employee_roles: (employee_id, role) (UNIQUE)
- employee_roles: employee_id
- employees: department_id
- leave_requests: employee_id
- leave_requests: status
- leave_requests: (employee_id, start_date, end_date)

### Flyway マイグレーション

| バージョン | 内容 |
|-----------|------|
| V3__create_departments.sql | departments テーブル作成 |
| V4__alter_employees_add_org.sql | employees に department_id 追加 |
| V5__create_employee_roles.sql | employee_roles テーブル作成 + 既存データ移行（EMPLOYEE → USER） |
| V6__create_leave_requests.sql | leave_requests テーブル作成 |

#### V5 の移行ロジック

```sql
-- 既存の role カラムから employee_roles へ変換
INSERT INTO employee_roles (employee_id, role)
SELECT id, CASE role WHEN 'EMPLOYEE' THEN 'USER' ELSE role END
FROM employees;
```

### ER 図（Unit-2 追加分）

```
┌──────────────┐       ┌──────────────────┐       ┌──────────────────┐
│ departments  │       │   employees      │       │ employee_roles   │
├──────────────┤       ├──────────────────┤       ├──────────────────┤
│ id (PK)      │1────N│ id (PK)          │1────N│ id (PK)          │
│ name (UQ)    │       │ email (UQ)       │       │ employee_id (FK) │
│ created_at   │       │ password         │       │ role             │
│ updated_at   │       │ name             │       └──────────────────┘
│ version      │       │ role (旧・互換)   │
└──────────────┘       │ department_id(FK)│
                       │ created_at       │
                       │ updated_at       │
                       │ version          │
                       └──────────────────┘
                              │
                              │1────N
                       ┌──────────────────┐
                       │ leave_requests   │
                       ├──────────────────┤
                       │ id (PK)          │
                       │ employee_id (FK) │
                       │ leave_type       │
                       │ leave_period_type│
                       │ start_date       │
                       │ end_date         │
                       │ reason           │
                       │ status           │
                       │ approved_by (FK) │
                       │ approved_at      │
                       │ rejection_reason │
                       │ created_at       │
                       │ updated_at       │
                       │ version          │
                       └──────────────────┘
```

---

## API 設計

### 部署管理 API（ADMIN のみ）

| Method | Path | 説明 |
|--------|------|------|
| GET | /api/departments | 部署一覧 |
| POST | /api/departments | 部署作成 |
| PUT | /api/departments/{id} | 部署更新 |
| DELETE | /api/departments/{id} | 部署削除（所属社員がいる場合は 409 Conflict） |

### 社員管理 API（ADMIN のみ）

| Method | Path | 説明 |
|--------|------|------|
| GET | /api/employees | 社員一覧 |
| POST | /api/employees | 社員登録 |
| PUT | /api/employees/{id} | 社員更新 |

### 休暇申請 API

| Method | Path | 説明 | ロール |
|--------|------|------|--------|
| POST | /api/leaves | 休暇申請作成 | USER / MANAGER |
| GET | /api/leaves/my | 自分の申請一覧 | USER / MANAGER |
| PUT | /api/leaves/{id}/cancel | 申請キャンセル | 申請者本人 |
| GET | /api/leaves/subordinates | 同一部署メンバーの申請一覧 | MANAGER |
| PUT | /api/leaves/{id}/approve | 承認 | MANAGER（申請者と同一部署） |
| PUT | /api/leaves/{id}/reject | 却下 | MANAGER（申請者と同一部署） |

### DTO

#### DepartmentRequest (record)

```java
public record DepartmentRequest(
    @NotBlank @Size(max = 100) String name
) {}
```

#### DepartmentResponse (record)

```java
public record DepartmentResponse(
    Long id,
    String name
) {}
```

#### EmployeeCreateRequest (record)

```java
public record EmployeeCreateRequest(
    @NotBlank String name,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password,
    @NotNull Long departmentId,
    @NotEmpty Set<Role> roles
) {}
```

#### EmployeeUpdateRequest (record)

```java
public record EmployeeUpdateRequest(
    @NotBlank String name,
    @NotNull Long departmentId,
    @NotEmpty Set<Role> roles
) {}
```

#### EmployeeDetailResponse (record)

```java
public record EmployeeDetailResponse(
    Long id,
    String name,
    String email,
    String departmentName,
    Long departmentId,
    Set<String> roles
) {}
```

#### LeaveCreateRequest (record)

```java
public record LeaveCreateRequest(
    @NotNull LeaveType leaveType,
    @NotNull LeavePeriodType leavePeriodType,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @Size(max = 500) String reason
) {}
```

#### LeaveResponse (record)

```java
public record LeaveResponse(
    Long id,
    Long employeeId,
    String employeeName,
    String leaveType,
    String leavePeriodType,
    String leavePeriodTypeDisplay,
    LocalDate startDate,
    LocalDate endDate,
    String reason,
    String status,
    String statusDisplay,
    String approvedByName,
    LocalDateTime approvedAt,
    String rejectionReason,
    LocalDateTime createdAt
) {}
```

#### LeaveRejectRequest (record)

```java
public record LeaveRejectRequest(
    @Size(max = 500) String reason
) {}
```

---

## Service 設計

| Service | 責務 |
|---------|------|
| DepartmentService | 部署の CRUD（削除時に所属社員チェック） |
| EmployeeService（拡張） | 社員登録・更新・一覧（ロール・部署の管理を含む） |
| LeaveService | 休暇申請・キャンセル・承認・却下・一覧取得 |

### LeaveService のビジネスロジック

#### 申請作成

1. 開始日 ≤ 終了日を検証
2. 同一期間に PENDING の申請があれば自動キャンセル（CANCELLED）
3. 同一期間に APPROVED の申請があれば自動キャンセル（CANCELLED）
4. 申請者が MANAGER ロールなら自動承認（status=APPROVED, approvedBy=自分）
5. それ以外は PENDING で作成

#### 承認

1. 対象申請が PENDING であることを検証（でなければ 409）
2. 操作者が MANAGER ロールかつ申請者と同一部署であることを検証（でなければ 403）
3. status を APPROVED に更新、approvedBy/approvedAt を記録

#### 却下

1. 対象申請が PENDING であることを検証（でなければ 409）
2. 操作者が MANAGER ロールかつ申請者と同一部署であることを検証（でなければ 403）
3. status を REJECTED に更新、approvedBy/approvedAt/rejectionReason を記録

#### キャンセル

1. 対象申請が PENDING であることを検証（でなければ 409）
2. 操作者が申請者本人であることを検証（でなければ 403）
3. status を CANCELLED に更新

---

## 画面設計

### 追加画面

| # | 画面名 | パス | ロール | 説明 |
|---|--------|------|--------|------|
| 9 | 部署管理 | /admin/departments | ADMIN | 部署の CRUD |
| 10 | 社員登録 | /admin/employees | ADMIN | 社員一覧 + 新規登録フォーム |
| 5' | 休暇申請（更新） | /leaves | USER / MANAGER | 一覧 + 新規申請 + キャンセル |
| 7' | 休暇承認（更新） | /manager/leaves | MANAGER | 部下の申請一覧 + 承認/却下 |

### 9. 部署管理画面

```
┌──────────────────────────────────────────┐
│ 部署管理                   [+ 新規追加]  │
├──────────────────────────────────────────┤
│ 部署名         | 操作                    │
│ 営業部         | [編集] [削除]           │
│ 開発部         | [編集] [削除]           │
│ 人事部         | [編集] [削除]           │
└──────────────────────────────────────────┘

新規追加 / 編集ダイアログ:
┌──────────────────────────────┐
│ 部署名: [____________]       │
│                              │
│ [保存]  [キャンセル]         │
└──────────────────────────────┘
```

### 10. 社員登録画面

```
┌──────────────────────────────────────────────────────┐
│ 社員管理                          [+ 新規登録]       │
├──────────────────────────────────────────────────────┤
│ 名前     | メール           | 部署   | ロール        │
│ 田中太郎 | tanaka@...       | 営業部 | USER          │
│ 佐藤花子 | sato@...         | 開発部 | USER          │
│ 管理者   | admin@...        | -      | ADMIN         │
└──────────────────────────────────────────────────────┘

新規登録ダイアログ:
┌──────────────────────────────────┐
│ 氏名:     [____________]         │
│ メール:   [____________]         │
│ パスワード:[____________]         │
│ 所属部署:  [営業部 ▼]            │
│ ロール:    ☑ USER ☐ MANAGER ☐ ADMIN │
│                                  │
│ [登録]  [キャンセル]             │
└──────────────────────────────────┘
```

### 5'. 休暇申請画面（更新）

```
┌──────────────────────────────────────────────────────────┐
│ 休暇申請                              [+ 新規申請]       │
├──────────────────────────────────────────────────────────┤
│ 種別   | 区分   | 期間          | 状態       | 操作      │
│ 有給   | 全日   | 7/20 - 7/20   | 申請中     | [取消]    │
│ 有給   | 午前休 | 8/1 - 8/1     | 承認       |           │
│ 有給   | 全日   | 8/13 - 8/15   | キャンセル |           │
└──────────────────────────────────────────────────────────┘

新規申請ダイアログ:
┌──────────────────────────────────┐
│ 種別:   [有給休暇 ▼]             │
│ 区分:   ○ 全日  ○ 午前休  ○ 午後休 │
│ 開始日: [2026-07-20]             │
│ 終了日: [2026-07-20]             │
│ 理由:   [____________]           │
│                                  │
│ [申請]  [キャンセル]             │
└──────────────────────────────────┘
```

### 7'. 休暇承認画面（MANAGER）

```
┌──────────────────────────────────────────────────────────────────┐
│ 休暇承認（同一部署メンバーの申請）                                │
├──────────────────────────────────────────────────────────────────┤
│ 申請者   | 種別 | 区分   | 期間          | 状態   | 操作         │
│ 田中太郎 | 有給 | 全日   | 7/20 - 7/20   | 申請中 | [承認] [却下]│
│ 佐藤花子 | 有給 | 午前休 | 8/1 - 8/1     | 申請中 | [承認] [却下]│
└──────────────────────────────────────────────────────────────────┘

却下ダイアログ（理由は任意）:
┌──────────────────────────────────┐
│ 却下理由（任意）:                │
│ [________________________]       │
│                                  │
│ [却下する]  [戻る]               │
└──────────────────────────────────┘
```

---

## ナビゲーション構成

| メニュー項目 | パス | 表示条件 |
|-------------|------|---------|
| ダッシュボード | / | 全員 |
| 勤怠一覧 | /attendance | 全員 |
| 休暇申請 | /leaves | USER / MANAGER |
| 休暇承認 | /manager/leaves | MANAGER |
| 部署管理 | /admin/departments | ADMIN |
| 社員管理 | /admin/employees | ADMIN |
| 勤怠管理 | /admin/attendance | ADMIN |
| レポート | /admin/reports | ADMIN |

---

## Repository

| Repository | 主なメソッド |
|-----------|-------------|
| DepartmentRepository | findAll, findById, existsByName, existsByIdAndEmployeesExist |
| EmployeeRoleRepository | findByEmployeeId, findByRole |
| LeaveRequestRepository | findByEmployeeId, findByEmployeeIdIn, findByStatus, findOverlapping |

### LeaveRequestRepository の特殊クエリ

```java
// 重複チェック: 指定期間に重なる PENDING または APPROVED 申請を検索
@Query("SELECT lr FROM LeaveRequest lr WHERE lr.employeeId = :employeeId " +
       "AND lr.status IN ('PENDING', 'APPROVED') " +
       "AND lr.startDate <= :endDate AND lr.endDate >= :startDate")
List<LeaveRequest> findOverlappingActive(Long employeeId, LocalDate startDate, LocalDate endDate);

// 同一部署メンバーの申請一覧（MANAGER が承認画面で使用）
@Query("SELECT lr FROM LeaveRequest lr JOIN Employee e ON lr.employeeId = e.id " +
       "WHERE e.departmentId = :departmentId AND lr.employeeId <> :managerId")
List<LeaveRequest> findByDepartmentExcluding(Long departmentId, Long managerId);
```

---

## セキュリティ

- 部署管理・社員管理 API: ADMIN ロールのみアクセス可
- 部署削除 API: 所属社員が存在する場合は 409 Conflict で拒否
- 休暇承認・却下 API: MANAGER ロールかつ申請者と同一部署であることを検証
- 休暇キャンセル API: 申請者本人のみ
- ロール判定は `employee_roles` テーブルから取得（JWT claims にロール一覧・部署IDを含める）
