# Unit 2: 休暇申請

## 概要

有給・休暇の申請機能と、管理者による承認/却下機能を実装する。

## 依存

- unit_0_foundation（Employee Entity, 認証基盤）

## ユーザーストーリー

- 社員として、休暇種別・期間を指定して休暇を申請したい
- 社員として、自分の休暇申請一覧と状態を確認したい
- 管理者として、申請中の休暇を承認または却下したい
- 管理者として、全社員の休暇申請一覧を確認したい

## スコープ

### Backend

- Flyway: `V8__create_leave_requests.sql`
- Entity: LeaveRequest
- Enum: LeaveType, LeaveStatus
- Repository: LeaveRequestRepository
- Service: LeaveService（interface + impl）
- Controller: LeaveController

### Frontend

- 休暇申請画面（`/leaves`）: 一覧 + 新規申請フォーム
- 管理者休暇承認画面（`/admin/leaves`）: 一覧 + 承認/却下ボタン

## テーブル

### leave_requests

```sql
CREATE TABLE leave_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    leave_type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by BIGINT REFERENCES employees(id),
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);
```

## API エンドポイント

| Method | Path | 説明 | ロール |
|--------|------|------|--------|
| POST | /api/leaves | 休暇申請作成 | 全員 |
| GET | /api/leaves?status | 休暇申請一覧 | 全員（自分の）/ ADMIN（全員の） |
| PUT | /api/leaves/{id}/approve | 承認 | ADMIN |
| PUT | /api/leaves/{id}/reject | 却下 | ADMIN |

## Enum

- `LeaveType`: PAID, SICK, OTHER
- `LeaveStatus`: PENDING, APPROVED, REJECTED

## ビジネスルール

- 開始日 ≤ 終了日（バリデーション）
- 過去日の申請は不可
- PENDING 状態のみ承認/却下可能（既に処理済みなら 409）
- 承認/却下は ADMIN ロールのみ
- 承認時に approvedBy, approvedAt を記録

## テスト観点

- LeaveService: 正常な申請が PENDING で作成される
- LeaveService: 開始日 > 終了日 で例外
- LeaveService: 承認で status が APPROVED に変わる
- LeaveService: 却下で status が REJECTED に変わる
- LeaveService: 処理済み申請の再承認で例外
- LeaveController: 各エンドポイントの正常系・異常系
- LeaveController: 一般社員が承認を試みると 403

## 完了基準

- [ ] 全テスト GREEN
- [ ] 申請 → 管理者承認/却下の E2E 動作
- [ ] テストカバレッジ 80% 以上
